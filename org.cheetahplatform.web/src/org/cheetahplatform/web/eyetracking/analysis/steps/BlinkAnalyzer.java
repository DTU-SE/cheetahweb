package org.cheetahplatform.web.eyetracking.analysis.steps;

import static org.cheetahplatform.web.eyetracking.cleaning.BlinkDetectionFilter.BLINK_BOTH;
import static org.cheetahplatform.web.eyetracking.cleaning.BlinkDetectionFilter.BLINK_COLUMN;
import static org.cheetahplatform.web.eyetracking.cleaning.BlinkDetectionFilter.BLINK_LEFT;
import static org.cheetahplatform.web.eyetracking.cleaning.BlinkDetectionFilter.BLINK_RIGHT;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cheetahplatform.web.eyetracking.analysis.AbstractTrialAnalyzer;
import org.cheetahplatform.web.eyetracking.analysis.DataProcessing;
import org.cheetahplatform.web.eyetracking.analysis.Trial;
import org.cheetahplatform.web.eyetracking.analysis.TrialDetectionNotification;
import org.cheetahplatform.web.eyetracking.analysis.TrialEvaluation;
import org.cheetahplatform.web.eyetracking.cleaning.PupillometryFile;
import org.cheetahplatform.web.eyetracking.cleaning.PupillometryFileColumn;
import org.cheetahplatform.web.eyetracking.cleaning.PupillometryFileLine;

public class BlinkAnalyzer extends AbstractTrialAnalyzer {

	public BlinkAnalyzer(AnalyzeConfiguration config, DataProcessing processing) {
		super(config, processing, AnalyzeStepType.BLINKS);
	}

	@Override
	public Map<String, String> analyze(TrialEvaluation trialEvaluation, PupillometryFile pupillometryFile) throws Exception {
		PupillometryFileColumn blinkColumn = pupillometryFile.getHeader().getColumn(BLINK_COLUMN);
		Map<String, String> results = new HashMap<>();
		if (blinkColumn == null) {
			trialEvaluation.setNotifications(Arrays.asList(new TrialDetectionNotification(
					"The input file does not contain a column named " + BLINK_COLUMN
							+ ". This is most likely since no blink detection was specified in the the clean step.",
					TrialDetectionNotification.TYPE_ERROR)));
			return results;
		}

		List<Trial> trials = trialEvaluation.getTrials();
		for (Trial trial : trials) {
			int blinks = 0;
			for (PupillometryFileLine line : trial.getLines()) {
				String blink = line.get(blinkColumn);
				if (blink != null && (blink.equals(BLINK_LEFT) || blink.equals(BLINK_RIGHT) || blink.equals(BLINK_BOTH))) {
					blinks++;
				}
			}

			String key = trial.getIdentifier() + RESULT_SEPARATOR + analyzeStep.getId();
			results.put(key, String.valueOf(blinks));
		}

		return results;
	}

}
