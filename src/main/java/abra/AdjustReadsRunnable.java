package abra;

import java.io.IOException;

public class AdjustReadsRunnable implements Runnable {
	
	private ReAligner realigner;
	private String sortedOriginalReads;
	private String sortedAlignedToContig;
	private String outputSam;
	private boolean isTightAlignment;
	private CompareToReference2 c2r;
	
//	public AdjustReadsRunnable(ReAligner realigner, String sortedOriginalReads, String sortedAlignedToContig, String outputSam,
//			boolean isTightAlignment) {
	public AdjustReadsRunnable(ReAligner realigner, String sortedAlignedToContig, String outputSam,
			boolean isTightAlignment, CompareToReference2 c2r) {

		this.realigner = realigner;
		this.sortedOriginalReads = sortedOriginalReads;
		this.sortedAlignedToContig = sortedAlignedToContig;
		this.outputSam = outputSam;
		this.isTightAlignment = isTightAlignment;
		this.c2r = c2r;
	}

	@Override
	public void run() {
		try {
			realigner.adjustReads(sortedAlignedToContig, outputSam, isTightAlignment, c2r);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}