package asset.pipeline.ratpack;

class AssetAttributes {
	private Boolean gzipExists = false;
	private boolean exists = false;
	private Long fileSize;
	private Long gzipFileSize;

	public AssetAttributes(boolean exists, Boolean gzipExists, Long fileSize, Long gzipFileSize) {
		this.gzipExists = gzipExists;
		this.exists = exists;
		this.fileSize = fileSize;
		this.gzipFileSize = gzipFileSize;
	}

	public boolean exists() {
		return this.exists;
	}

	public Boolean gzipExists() {
		return this.gzipExists;
	}

	public Long getFileSize() {
		return this.fileSize;
	}

	public Long getGzipFileSize() {
		return this.gzipFileSize;
	}
}