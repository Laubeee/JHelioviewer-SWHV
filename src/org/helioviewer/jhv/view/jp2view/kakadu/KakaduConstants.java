package org.helioviewer.jhv.view.jp2view.kakadu;

public class KakaduConstants {

    public static final int KDU_PRECINCT_DATABIN = 0;
    public static final int KDU_TILE_HEADER_DATABIN = 1;
    public static final int KDU_TILE_DATABIN = 2;
    public static final int KDU_MAIN_HEADER_DATABIN = 3;
    public static final int KDU_META_DATABIN = 4;
    public static final int KDU_UNDEFINED_DATABIN = 5;
    // Maximum of samples to process per rendering iteration
    public static final int MAX_RENDER_SAMPLES = 256 * 1024;
    // The amount of cache to allocate to each codestream
    public static final int CODESTREAM_CACHE_THRESHOLD = 1024 * 1024;

}
