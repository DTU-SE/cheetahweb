package org.cheetahplatform.web.eyetracking.analysis.steps;

import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.UnivariateStatistic;
import org.cheetahplatform.web.dto.ReportableResultEntry;
import org.cheetahplatform.web.eyetracking.analysis.AbstractPupilTrialAnalyzer;
import org.cheetahplatform.web.eyetracking.analysis.Baseline;
import org.cheetahplatform.web.eyetracking.analysis.DataProcessing;
import org.cheetahplatform.web.eyetracking.analysis.DataProcessingStep;
import org.cheetahplatform.web.eyetracking.analysis.Stimulus;
import org.cheetahplatform.web.eyetracking.analysis.Trial;
import org.cheetahplatform.web.eyetracking.cleaning.PupillometryFile;
import org.cheetahplatform.web.eyetracking.cleaning.PupillometryFileColumn;
import org.cheetahplatform.web.eyetracking.cleaning.PupillometryFileLine;

public class RelativeDividedTrialAnalyzer extends AbstractPupilTrialAnalyzer {

	private UnivariateStatistic statistic;

	public RelativeDividedTrialAnalyzer(AnalyzeConfiguration config, DataProcessing processing, AnalyzeStepType type,
			UnivariateStatistic statistic, long startTime, long endTime, DataProcessingStep dataProcessingStep) {
		super(config, processing, type, startTime, endTime, dataProcessingStep);

		this.statistic = statistic;
	}

	@Override
	protected void analyzeTrial(PupillometryFile file, Trial trial, Map<String, List<ReportableResultEntry>> results,
			PupillometryFileColumn leftPupilColumn, PupillometryFileColumn rightPupilColumn) {
		Baseline baseline = trial.getBaseline();
		if (baseline == null || baseline.getLines().isEmpty()) {
			return;
		}

		Stimulus stimulus = trial.getStimulus();
		if (stimulus == null || stimulus.getLines().isEmpty()) {
			return;
		}

		double leftBaseline = calculateMean(baseline.getLines(), leftPupilColumn);
		double rightBaseline = calculateMean(baseline.getLines(), rightPupilColumn);

		double leftValue = 0;
		if (leftBaseline > 0) {
			double[] leftPupils = divideByBaseline(file, stimulus.getLines(), leftPupilColumn, leftBaseline);
			leftValue = statistic.evaluate(leftPupils);
			addResult(results, trial, leftValue, PUPIL_LEFT);
		}

		double rightValue = 0;
		if (rightBaseline > 0) {
			double[] rightPupils = divideByBaseline(file, stimulus.getLines(), rightPupilColumn, rightBaseline);
			rightValue = statistic.evaluate(rightPupils);
			addResult(results, trial, rightValue, PUPIL_RIGHT);
		}

		addAveragePupilSizeToResults(trial, results, leftValue, rightValue);
	}

	private double[] divideByBaseline(PupillometryFile file, List<PupillometryFileLine> lines, PupillometryFileColumn leftPupilColumn,
			double leftBaseline) {
		double[] leftPupils = getPupilValues(file, leftPupilColumn, lines);
		for (int i = 0; i < leftPupils.length; i++) {
			leftPupils[i] = leftPupils[i] / leftBaseline;
		}
		return leftPupils;
	}
}
