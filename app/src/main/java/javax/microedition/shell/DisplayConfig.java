/*
 *  Che779 Emulator - cấu hình hiển thị theo tab (trước đây là static trong Canvas/Displayable).
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 */
package javax.microedition.shell;

import ru.playsoftware.j2meloader.config.ShaderInfo;

/** Cài đặt màn hình của một tab, để các tab hiện cạnh nhau trong lưới có thể khác nhau. */
public final class DisplayConfig {
	public volatile boolean filter;
	public volatile boolean touchInput;
	public volatile int graphicsMode;
	public volatile ShaderInfo shaderFilter;
	public volatile boolean parallelRedraw;
	public volatile boolean forceFullscreen;
	public volatile boolean showFps;
	public volatile int backgroundColor;
	public volatile int scaleRatio;
	public volatile int fpsLimit;
	public volatile int scaleType;
	public volatile int screenGravity;
	public volatile int virtualWidth;
	public volatile int virtualHeight;
}
