package org.cheetahplatform.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.cheetahplatform.web.dto.CheetahWorkItemDto;
import org.cheetahplatform.web.dto.CheetahWorkerDto;

public class CheetahWorker extends Thread {
	private static final String IDLE_STATE = "IDLE";
	private static final String BUSY_STATE = "BUSY";
	private static final String IDLE_MESSAGE = "Nothing to do here. Life is good :)";
	private static final String BUSY_MESSAGE = "Working hard on the following task: ";

	/**
	 * Queue containing all workers that are scheduled for work.
	 */
	private static final ConcurrentLinkedDeque<ICheetahWorkItem> WORK_QUEUE = new ConcurrentLinkedDeque<ICheetahWorkItem>();
	/**
	 * Map with all workers that are currently working - mapping of id to worker.
	 */
	private static final Map<Long, ICheetahWorkItem> ACTIVE_WORKERS = new ConcurrentHashMap<Long, ICheetahWorkItem>();
	private static long idCounter = 1;

	private static boolean canAlter(long userId, ICheetahWorkItem workItem) {
		if (workItem != null) {
			if (userId == workItem.getUserId()) {
				return true;
			}
			return false;
		}

		return false;
	}

	public static boolean canCancel(long userId, CheetahWorkItemDto workItemDto) {
		ICheetahWorkItem workItem = findWorkItem(workItemDto);
		return canAlter(userId, workItem);
	}

	public static boolean cancel(long userId, CheetahWorkItemDto workItemDto) {
		ICheetahWorkItem workItem = findWorkItem(workItemDto);
		if (!canAlter(userId, workItem)) {
			return false;
		}

		boolean workItemRemoved = WORK_QUEUE.remove(workItem);
		if (workItemRemoved) {
			workItem.cancel();
		}
		return workItemRemoved;
	}

	private static synchronized long createWorkItemId() {
		return idCounter++;
	}

	public static List<CheetahWorkItemStatus> determineWorkItemStatus(Long... workItemIds) {
		Set<Long> filter = new HashSet<>(Arrays.asList(workItemIds));
		List<CheetahWorkItemStatus> result = new ArrayList<>();
		Iterator<ICheetahWorkItem> iterator = WORK_QUEUE.iterator();

		while (iterator.hasNext()) {
			ICheetahWorkItem cheetahWorkItem = iterator.next();
			if (filter.contains(cheetahWorkItem.getId())) {
				result.add(new CheetahWorkItemStatus(cheetahWorkItem.getId(), "waiting"));
			}
		}
		for (Long id : workItemIds) {
			ICheetahWorkItem workItem = ACTIVE_WORKERS.get(id);
			if (workItem != null) {
				result.add(new CheetahWorkItemStatus(id, "working"));
			}
		}
		for (CheetahWorkItemStatus status : result) {
			filter.remove(status.getId());
		}
		for (Long finishedId : filter) {
			result.add(new CheetahWorkItemStatus(finishedId, "finished"));
		}

		return result;
	}

	private static ICheetahWorkItem findWorkItem(CheetahWorkItemDto workItemDto) {
		Iterator<ICheetahWorkItem> iterator = WORK_QUEUE.iterator();
		while (iterator.hasNext()) {
			ICheetahWorkItem iCheetahWorkItem = iterator.next();
			if (workItemDto.getId() == iCheetahWorkItem.getId()) {
				return iCheetahWorkItem;
			}
		}
		return null;
	}

	public static List<CheetahWorkItemDto> getScheduledWorkItems(long userId) {
		List<CheetahWorkItemDto> list = new ArrayList<>();
		for (ICheetahWorkItem workItem : WORK_QUEUE) {
			String displayName = workItem.getDisplayName();
			CheetahWorkItemDto dto = new CheetahWorkItemDto(workItem.getId(), displayName, workItem.getUserId() == userId);
			list.add(dto);
		}

		return list;
	}

	public static int getWorkQueueSize() {
		return WORK_QUEUE.size();
	}

	public static boolean moveToTop(long userId, CheetahWorkItemDto cheetahWorkItemDto) {
		ICheetahWorkItem workItem = findWorkItem(cheetahWorkItemDto);
		if (workItem == null) {
			return false;
		}

		if (!canAlter(userId, workItem)) {
			return false;
		}
		boolean removed = WORK_QUEUE.remove(workItem);
		if (removed) {
			WORK_QUEUE.offerFirst(workItem);
		}
		return true;
	}

	public static void schedule(ICheetahWorkItem workItem) {
		workItem.setId(createWorkItemId());
		WORK_QUEUE.add(workItem);
	}

	private boolean terminated = false;

	private String message = IDLE_MESSAGE;

	public void doShutdown() {
		terminated = true;
	}

	public CheetahWorkerDto getMessage() {
		String status = BUSY_STATE;
		if (IDLE_MESSAGE.equals(message)) {
			status = IDLE_STATE;
		}
		return new CheetahWorkerDto(message, status);
	}

	@Override
	public void run() {
		while (!terminated) {
			ICheetahWorkItem workItem = WORK_QUEUE.poll();
			while (workItem != null) {
				try {
					message = BUSY_MESSAGE + workItem.getDisplayName();
					ACTIVE_WORKERS.put(workItem.getId(), workItem);
					workItem.doWork();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					ACTIVE_WORKERS.remove(workItem.getId());
				}
				workItem = WORK_QUEUE.poll();
			}
			message = IDLE_MESSAGE;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// nothing to do
			}
		}
	}
}
