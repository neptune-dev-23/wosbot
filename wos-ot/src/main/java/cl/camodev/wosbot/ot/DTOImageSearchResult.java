package cl.camodev.wosbot.ot;

public class DTOImageSearchResult {
	private boolean found;
	private DTOPoint matchLocation;

	public DTOImageSearchResult(boolean found, DTOPoint matchLocation) {
		this.found = found;
		this.matchLocation = matchLocation;
	}

	public boolean isFound() {
		return found;
	}

	public DTOPoint getMatchLocation() {
		return matchLocation;
	}

	@Override
	public String toString() {
		return "DTOImageSearchResult [found=" + found + ", matchLocation=" + matchLocation + "]";
	}
}
