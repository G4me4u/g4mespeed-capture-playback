package com.g4mesoft.captureplayback.gui;

import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSSignalTime;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrack;
import com.g4mesoft.captureplayback.module.GSCapturePlaybackModule;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.panel.GSPanelContext;
import com.g4mesoft.panel.GSParentPanel;
import com.g4mesoft.panel.legend.GSButtonPanel;

import net.minecraft.util.math.BlockPos;

public class GSCapturePlaybackPanel extends GSParentPanel {

	private static final int TOP_MARGIN = 5;

	private GSButtonPanel editCompositionButton;
	private GSButtonPanel editSequenceButton;

	public GSCapturePlaybackPanel(GSCapturePlaybackModule module) {
		editCompositionButton = new GSButtonPanel("Edit Composition", () -> {
			GSComposition composition = new GSComposition(UUID.randomUUID(), "Test");

			GSSequence sequence1 = new GSSequence(UUID.randomUUID(), "Sequence #1");
			sequence1.addChannel(new GSChannelInfo("", 0xFFFF00FF, BlockPos.ORIGIN))
				.tryAddEntry(new GSSignalTime(0, 0), new GSSignalTime(20, 0));
			
			GSSequence sequence2 = new GSSequence(UUID.randomUUID(), "Sequence #2");
			sequence2.addChannel(new GSChannelInfo("", 0xFFFF00FF, BlockPos.ORIGIN))
				.tryAddEntry(new GSSignalTime(0, 0), new GSSignalTime(20, 0));
			
			GSSequence sequence3 = new GSSequence(UUID.randomUUID(), "Sequence #3");
			sequence3.addChannel(new GSChannelInfo("", 0xFFFF00FF, BlockPos.ORIGIN))
				.tryAddEntry(new GSSignalTime(0, 0), new GSSignalTime(20, 0));
			
			GSSequence sequence4 = new GSSequence(UUID.randomUUID(), "Sequence #4");
			sequence4.addChannel(new GSChannelInfo("", 0xFFFF00FF, BlockPos.ORIGIN))
				.tryAddEntry(new GSSignalTime(0, 0), new GSSignalTime(30, 0));
			
			GSSequence sequence5 = new GSSequence(UUID.randomUUID(), "Sequence #5");
			sequence5.addChannel(new GSChannelInfo("", 0xFFFF00FF, BlockPos.ORIGIN))
				.tryAddEntry(new GSSignalTime(0, 0), new GSSignalTime(80, 0));
			
			composition.addSequence(sequence1);
			composition.addSequence(sequence2);
			composition.addSequence(sequence3);
			composition.addSequence(sequence4);
			composition.addSequence(sequence5);
			
			GSTrack track1 = composition.addTrack("Track #1", 0xFFBBEEAA);
			track1.addEntry(sequence1.getSequenceUUID(), 0);
			track1.addEntry(sequence2.getSequenceUUID(), 20);
			track1.addEntry(sequence3.getSequenceUUID(), 40);
			
			GSTrack track2 = composition.addTrack("Track #2", 0xFFEE22EE);
			track2.addEntry(sequence3.getSequenceUUID(), 0);
			track2.addEntry(sequence2.getSequenceUUID(), 20);
			
			GSTrack track3 = composition.addTrack("Track #3", 0xFFEE8822);
			track3.addEntry(sequence4.getSequenceUUID(), 0);
			track3.addEntry(sequence4.getSequenceUUID(), 30);
			track3.addEntry(sequence4.getSequenceUUID(), 60);
			
			GSTrack track4 = composition.addTrack("Track #4", 0xFFFFFFFF);
			track4.addEntry(sequence5.getSequenceUUID(), 15);
			
			GSPanelContext.setContent(new GSCompositionEditPanel(composition));
		});
		
		editSequenceButton = new GSButtonPanel("Edit Sequence", () -> {
			GSPanelContext.setContent(new GSSequenceEditPanel(module.getActiveSequence()));
		});
		
		add(editCompositionButton);
		add(editSequenceButton);
	}
	
	@Override
	protected void layout() {
		editCompositionButton.setPreferredBounds(width / 2 - 95, TOP_MARGIN, 90);
		editSequenceButton.setPreferredBounds(width / 2 + 5, TOP_MARGIN, 90);
	}
}
