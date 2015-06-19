package asset.pipeline.ratpack;

public class AssetAttributes {
	private Boolean gzipExists = false;
	private boolean exists = false;
	private boolean isDirectory = false;
	private Long fileSize;
	private Long gzipFileSize;

	public AssetAttributes(boolean exists, Boolean gzipExists, Boolean isDirectory, Long fileSize, Long gzipFileSize) {
		this.gzipExists = gzipExists;
		this.exists = exists;
		this.fileSize = fileSize;
		this.isDirectory = isDirectory;
		this.gzipFileSize = gzipFileSize;
	}

	public boolean exists() {
		return this.exists;
	}

	public boolean isDirectory() {
		return this.isDirectory;
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