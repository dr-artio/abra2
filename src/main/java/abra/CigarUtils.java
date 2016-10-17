package abra;

import java.util.ArrayList;
import java.util.List;


public class CigarUtils {
	
	/**
	 * Extract subset of cigar string based upon input position (index) into cigar and length. 
	 */
	public static int subsetCigarString(int pos, int length, String cigar, StringBuffer newCigar) {
		List<CigarBlock> cigarBlocks = getCigarBlocks(cigar);
		List<CigarBlock> newCigarBlocks = new ArrayList<CigarBlock>();
		int relativeRefPos = subsetCigarBlocks(cigarBlocks, pos, length, newCigarBlocks);
		
		for (CigarBlock block : newCigarBlocks) {
			newCigar.append(block.length);
			newCigar.append(block.type);
		}
		
		return relativeRefPos;
	}
	
	private static String cigarStringFromCigarBlocks(List<CigarBlock> blocks) {
		StringBuffer newCigar = new StringBuffer();
		
		for (CigarBlock block : blocks) {
			newCigar.append(block.length);
			newCigar.append(block.type);
		}
		
		return newCigar.toString();
	}
	
	public static String extendCigarWithMatches(String cigar, int leftPad, int rightPad) {
		List<CigarBlock> blocks = getCigarBlocks(cigar);
		
		if (blocks.get(0).type == 'M') {
			blocks.get(0).length += leftPad;
		} else {
			blocks.add(0, new CigarBlock(leftPad, 'M'));
		}
		
		int lastBlockIdx = blocks.size()-1;
		
		if (blocks.get(lastBlockIdx).type == 'M') {
			blocks.get(lastBlockIdx).length += rightPad;
		} else {
			blocks.add(new CigarBlock(rightPad, 'M'));
		}
		
		return cigarStringFromCigarBlocks(blocks);
	}
	
	public static String injectSplice(String cigar, int junctionPos, int junctionLength) {
		
		// Identify pos relative to reference and insert N element
		List<CigarBlock> blocks = getCigarBlocks(cigar);
		List<CigarBlock> newBlocks = new ArrayList<CigarBlock>();
		int refPos = 0;

		for (CigarBlock block : blocks) {
			if (block.type == 'M' || block.type == 'D') {
				if (refPos < junctionPos && refPos + block.length >= junctionPos) {
					// Split up current block into 2 blocks with splice block in between
					int blockLen1 = junctionPos - refPos;
					int blockLen2 = block.length - blockLen1;
					newBlocks.add(new CigarBlock(blockLen1, block.type));
					newBlocks.add(new CigarBlock(junctionLength, 'N'));
					if (blockLen2 > 0) {
						newBlocks.add(new CigarBlock(blockLen2, block.type));
					}
					
					refPos += block.length;
				} else {
					newBlocks.add(block);
					refPos += block.length;
				}
			} else {
				// Do not advance ref pos for insertions
				newBlocks.add(block);
			}
		}
		
		return cigarStringFromCigarBlocks(newBlocks);
	}

	private static List<CigarBlock> getCigarBlocks(String cigar) {
		
		List<CigarBlock> cigarBlocks = new ArrayList<CigarBlock>();
		
		StringBuffer len = new StringBuffer();
		for (int i=0; i<cigar.length(); i++) {
			char ch = cigar.charAt(i);
			if (Character.isDigit(ch)) {
				len.append(ch);
			} else {
				cigarBlocks.add(new CigarBlock(Integer.valueOf(len.toString()), ch));
				len.setLength(0);
			}
		}
		
		return cigarBlocks;
	}
	
	private static int subsetCigarBlocks(List<CigarBlock> contigBlocks, int pos, int readLength, List<CigarBlock> readBlocks) {
		int currLen = 0;
		int contigPos = 0;
		int relativeRefPos = 0;
		boolean isReadPosReached = false;
//		List<CigarBlock> readBlocks = new ArrayList<CigarBlock>();
		
		for (CigarBlock block : contigBlocks) {
			
			int blockLength = block.length;
			
			// Identify the start point for subsetting
			if (!isReadPosReached) {
				if (!block.isGap()) {  // Never start in a deletion
					if (contigPos + block.length >= pos) {
						blockLength = contigPos + block.length - pos;
						isReadPosReached = true;
						
						if (block.type != 'I') {
							// Include partial block length for matches
							relativeRefPos += block.length - blockLength;
						}
					} else {
						contigPos += block.length;
						if (block.type != 'I') {
							// Include entire block for matches
							relativeRefPos += block.length;
						}
					}
				} else {
					// Include entire block for deletes
					relativeRefPos += block.length;
				}				
			} 
			
			if (isReadPosReached && blockLength > 0) {
				if (block.isGap()) {
					// Never start in a deletion
					if (!readBlocks.isEmpty()) {
						readBlocks.add(block);
					} else {
						// skip over leading deletion in reference position
						relativeRefPos += block.length;
					}
				}
				else if (blockLength < readLength-currLen) {
					currLen += blockLength;
					readBlocks.add(new CigarBlock(blockLength, block.type));
				} else {
					int len = readLength - currLen;
					currLen += len;
					readBlocks.add(new CigarBlock(len, block.type));
					break;
				}
			}
		}
		
		return relativeRefPos;
	}
	
	static class CigarBlock {
		int length;
		char type;
		
		CigarBlock(int length, char type) {
			this.length = length;
			this.type = type;
		}
		
		boolean isGap() {
			return type == 'D' || type == 'N';
		}
	}

}
