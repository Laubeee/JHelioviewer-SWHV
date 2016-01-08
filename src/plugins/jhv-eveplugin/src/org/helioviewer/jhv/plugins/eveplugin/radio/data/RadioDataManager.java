package org.helioviewer.jhv.plugins.eveplugin.radio.data;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.base.Range;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.plugins.eveplugin.EVEState;
import org.helioviewer.jhv.plugins.eveplugin.draw.PlotAreaSpace;
import org.helioviewer.jhv.plugins.eveplugin.radio.model.RadioPlotModel;
import org.helioviewer.jhv.plugins.eveplugin.radio.model.RadioYAxisElement;
import org.helioviewer.jhv.plugins.eveplugin.radio.model.ResolutionSetting;
import org.helioviewer.jhv.plugins.eveplugin.radio.model.ZoomManager;
import org.helioviewer.jhv.plugins.eveplugin.settings.EVESettings;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;
import org.helioviewer.jhv.viewmodel.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2Image.ReaderMode;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2ImageCallisto;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2ViewCallisto;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet.ResolutionLevel;

/**
 * The radio data manager manages all the downloaded data for radio
 * spectrograms.
 *
 * It receives all of its input from the radio downloader as listener of the
 * radio downloader.
 *
 * @author Bram.Bourgoignie@oma.be
 *
 */
public class RadioDataManager implements RadioDownloaderListener {

    /** The singleton instance of the class. */
    private static RadioDataManager instance;

    /** A list collecting all the radio data manager listeners. */
    private List<RadioDataManagerListener> listeners;

    /** A map keeping the download request data. */
    private DownloadRequestData downloadRequestData;

    /** Instance of the radio downloader */
    private RadioDownloader downloader;

    /** A buffer holding all the requests for data. */
    private RequestForDataBuffer requestBuffer;

    /** The instance of the line data selector model */
    private LineDataSelectorModel lineDataSelectorModel;

    /** Instance of the radio image cache */
    private RadioImageCache cache;

    /** Instance of the zoom manager */
    private ZoomManager zoomManager;

    /** Instance of eve state */
    private EVEState eveState;

    /** Is there a request for data busy */
    private boolean requestForDataBusy;

    /**
     * private constructor used when the instance is requested for the first
     * time.
     *
     */
    private RadioDataManager() {
    }

    /**
     * Access to the singleton instance of the radio data manager
     *
     * @return The instance of the radio data manager
     */
    public static RadioDataManager getSingletonInstance() {
        if (instance == null) {
            instance = new RadioDataManager();
            instance.init();
        }
        return instance;
    }

    private void init() {
        listeners = new ArrayList<RadioDataManagerListener>();
        downloader = RadioDownloader.getSingletonInstance();
        downloader.addRadioDownloaderListener(this);
        lineDataSelectorModel = LineDataSelectorModel.getSingletonInstance();
        cache = RadioImageCache.getInstance();
        requestBuffer = new RequestForDataBuffer();
        zoomManager = ZoomManager.getSingletonInstance();
        eveState = EVEState.getSingletonInstance();
        requestForDataBusy = false;
    }

    /**
     * Adds a radio data manager listener.
     *
     * @param l
     *            The listener to add
     */
    public void addRadioManagerListener(RadioDataManagerListener l) {
        listeners.add(l);
    }

    /**
     * Removes a radio data manager listener.
     *
     * @param l
     *            The listener to remove
     */
    public void removeRadioManagerListener(RadioDataManagerListener l) {
        listeners.remove(l);
    }

    /**
     * Inform the RadioDataManager about the new data that was received. The
     * RadioDataManagerListeners will be informed about the new received data.
     *
     *
     * @param data
     *            The data received
     * @param imageID
     *            The ID of the image for which data was received
     * @param downloadID
     *            The ID of the download batch the image is part of
     * @param dataSize
     *            The height and width of the data
     */
    public void dataForIDReceived(byte[] byteData, long imageID, Rectangle dataSize, Rectangle providedRegion, int resolutionHight) {
        fireDataforIDReceived(byteData, imageID, dataSize, providedRegion, resolutionHight);
    }

    /**
     * Removes the given download request data. The data is removed from the
     * line selector model. The images are removed from the cache.
     *
     * The listeners are informed about the removal of the download request
     * data.
     *
     * @param drd
     *            The download request data that should be removed
     */
    public void removeDownloadRequestData() {
        if (downloadRequestData != null) {
            lineDataSelectorModel.removeLineData(downloadRequestData);
            for (long imageID : downloadRequestData.getRadioImages().keySet()) {
                cache.remove(imageID);
            }
            fireDownloadRequestDataRemoved(downloadRequestData);
            downloadRequestData = null;

            PlotAreaSpace.getSingletonInstance().resetSelectedValueAndTimeInterval();
        }
    }

    /**
     * Changed the visibility of the download request data. All radio data
     * manager listeners are informed.
     *
     *
     * @param drd
     *            The download request data for which the visiblility is changed
     */
    public void downloadRequestDataVisibilityChanged(DownloadRequestData drd) {
        fireDownloadRequestDataVisibilityChanged(drd);
    }

    /**
     * Request the radio data manager for new data.
     *
     * As long as the mouse is dragged no new data is requested. This to improve
     * the responsiveness of the program.
     *
     * This function can be called asynchronously. A request buffer is used to
     * store the all the calls. As long as there are requests in the buffer
     * function continues.
     *
     * @param xStart
     *            The start time for which new data is requested
     * @param xEnd
     *            The end time for which new data is requested
     * @param yStart
     *            The start frequency for which new data is requested
     * @param yEnd
     *            The end frequency for which new data is requested
     * @param xRatio
     *            The x-ratio (time per pixel)
     * @param yRatio
     *            The y-ratio (frequency per pixel)
     * @param iDs
     *            The download ID-s for which new data is requested
     * @param plotIdentifier
     *            The identifier of the plot for which new data is requested
     */
    public void requestForData(Date xStart, Date xEnd, double yStart, double yEnd, double xRatio, double yRatio) {
        if (!eveState.isMouseTimeIntervalDragging() && !eveState.isMouseValueIntervalDragging()) {
            if (!requestBuffer.hasData() && !requestForDataBusy) {
                requestForDataBusy = true;
                requestBuffer.addRequestConfig(new RequestConfig(xStart, xEnd, yStart, yEnd, xRatio, yRatio));
                while (requestBuffer.hasData()) {
                    RequestConfig requestConfig = requestBuffer.getData();
                    handleRequestConfig(requestConfig, xStart, xEnd, yStart, yEnd);
                }
                requestForDataBusy = false;
            } else {
                requestBuffer.addRequestConfig(new RequestConfig(xStart, xEnd, yStart, yEnd, xRatio, yRatio));
            }
        }
    }

    /**
     * Informs the radio data manager about an image, identified by imageID from
     * download identified by downloadID, finished its downloading?
     *
     *
     * @param imageID
     *            The image id of the image that finished downloading
     * @param downloadID
     *            The download id of the batch of which the image was part
     */
    public void finishedDownloadingID(long imageID) {
        if (downloadRequestData != null) {
            RadioImage image = downloadRequestData.getRadioImages().get(imageID);
            if (image != null) {
                image.setDownloading(false);
            }
            boolean isDownloading = false;
            for (RadioImage im : downloadRequestData.getRadioImages().values()) {
                if (im.isDownloading()) {
                    isDownloading = true;
                    break;
                }
            }
            if (!isDownloading) {
                if (downloadRequestData.isDownloading()) {
                    downloadRequestData.setDownloading(false);
                    lineDataSelectorModel.downloadFinished(downloadRequestData);
                }
            }
        }
    }

    /*
     * RadioDownloadListener
     */
    @Override
    public void intervalTooBig(Date requestedStartTime, Date requestedEndTime) {
        DownloadRequestData downloadRequestData = new DownloadRequestData();
        lineDataSelectorModel.addLineData(downloadRequestData);
        fireIntervalTooBig();
        fireNewDataAvailable(downloadRequestData);
        fireDownloadRequestAnswered(new Interval<Date>(requestedStartTime, requestedEndTime));
    }

    @Override
    public void newJPXFilesDownloaded(List<DownloadedJPXData> jpxFiles, Date requestedStartTime, Date requestedEndTime) {
        downloadRequestData = new DownloadRequestData();
        downloadRequestData.setDownloading(true);
        lineDataSelectorModel.addLineData(downloadRequestData);
        if (!jpxFiles.isEmpty()) {
            for (DownloadedJPXData djd : jpxFiles) {
                handleDownloadedJPXData(djd, downloadRequestData, Double.NaN, Double.NaN);
            }
        }
        defineMaxBounds();
        fireNewDataAvailable(downloadRequestData);
        fireDownloadRequestAnswered(new Interval<Date>(requestedStartTime, requestedEndTime));
    }

    @Override
    public void newAdditionalDataDownloaded(List<DownloadedJPXData> jpxFiles, double ratioX, double ratioY) {
        if (downloadRequestData != null) {
            boolean oldDownloading = downloadRequestData.isDownloading();
            downloadRequestData.setDownloading(true);
            if (!oldDownloading) {
                lineDataSelectorModel.downloadStarted(downloadRequestData);
            }
            for (DownloadedJPXData djd : jpxFiles) {
                handleDownloadedJPXData(djd, downloadRequestData, ratioX, ratioY);
            }
            defineMaxBounds();
            fireNewDataAvailable(downloadRequestData);
        }
    }

    @Override
    public void newNoData(List<Interval<Date>> noDataList) {
        if (!eveState.isMouseTimeIntervalDragging() && !eveState.isMouseValueIntervalDragging() && noDataList.size() > 0) {
            fireNoDataIntervalsReceived(noDataList);
        }
    }

    @Override
    public void removeSpectrograms() {
        removeDownloadRequestData();
    }

    /**
     * Define the maximum bound of the frequency interval. The y-value model is
     * updated.
     *
     * @param downloadID
     *            The download ID for which the bounds are defined
     * @param plotIdentifier
     *            The plot identifier for which the bound are defined
     */
    private void defineMaxBounds() {
        FrequencyInterval maxFrequencyInterval = new FrequencyInterval();
        if (downloadRequestData != null) {
            Map<Long, RadioImage> radioImages = downloadRequestData.getRadioImages();
            if (!radioImages.isEmpty()) {
                int localMinFrequency = -1;
                int localMaxFrequency = -1;
                boolean first = true;
                for (RadioImage image : radioImages.values()) {
                    if (first) {
                        localMinFrequency = image.getFreqInterval().getStart();
                        localMaxFrequency = image.getFreqInterval().getEnd();
                        first = false;
                    } else {
                        if (image.getFreqInterval().getStart() < localMinFrequency) {
                            localMinFrequency = image.getFreqInterval().getStart();
                        }
                        if (image.getFreqInterval().getEnd() > localMaxFrequency) {
                            localMaxFrequency = image.getFreqInterval().getEnd();
                        }
                    }
                }
                maxFrequencyInterval = new FrequencyInterval(localMinFrequency, localMaxFrequency);
            } else {
                maxFrequencyInterval = new FrequencyInterval(0, 0);
            }
        }
        RadioYAxisElement yAxisElement = RadioPlotModel.getSingletonInstance().getYAxisElement();
        yAxisElement.setAvailableRange(new Range(maxFrequencyInterval.getStart(), maxFrequencyInterval.getEnd()));
    }

    /**
     * Informs all RadioDataManagerListener of new received data.
     *
     * @param data
     *            The data received
     * @param imageID
     *            The ID of the image for which data was received
     * @param downloadID
     *            The ID of the download batch the image is part of
     * @param dataSize
     *            The height and width of the data
     */
    private void fireDataforIDReceived(int[] data, long imageID, Rectangle dataSize) {
        if (downloadRequestData != null) {
            RadioImage image = downloadRequestData.getRadioImages().get(imageID);
            if (image != null) {
                image.setLastDataSize(dataSize);
                if (image.getVisibleImageFreqInterval() != null && image.getVisibleImageTimeInterval() != null) {
                    for (RadioDataManagerListener l : listeners) {
                        l.newDataForIDReceived(data, image.getVisibleImageTimeInterval(), image.getVisibleImageFreqInterval(), image.getFreqInterval(), dataSize, imageID);
                    }
                }
            } else {
                // Log.debug("The image was null");
            }
        } else {
            // Log.debug("Download request data was null");
        }
    }

    private void fireDataforIDReceived(byte[] byteData, long imageID, Rectangle dataSize, Rectangle providedRegion, int resolutionHeight) {
        if (downloadRequestData != null) {
            RadioImage image = downloadRequestData.getRadioImages().get(imageID);
            if (image != null) {
                image.setLastDataSize(dataSize);
                if (image.getVisibleImageFreqInterval() != null && image.getVisibleImageTimeInterval() != null) {
                    FrequencyInterval dataFrequencyInterval = defineDataFrequencyInterval(image.getFreqInterval(), providedRegion, resolutionHeight);
                    for (RadioDataManagerListener l : listeners) {
                        l.newDataForIDReceived(byteData, image.getVisibleImageTimeInterval(), dataFrequencyInterval, image.getFreqInterval(), dataSize, imageID);
                    }
                }
            } else {
                // Log.debug("The image was null");
            }
        } else {
            // Log.debug("Download request data was null");
        }
    }

    private FrequencyInterval defineDataFrequencyInterval(FrequencyInterval freqInterval, Rectangle providedRegion, int resolutionHeight) {
        double ratio = (freqInterval.getEnd() - freqInterval.getStart()) / resolutionHeight;
        double start = freqInterval.getEnd() - providedRegion.getY() * ratio;
        double end = freqInterval.getEnd() - (providedRegion.getY() + providedRegion.getHeight()) * ratio;
        return new FrequencyInterval((int) start, (int) end);
    }

    /**
     * Informs the radio data manager listeners about new available data.
     *
     * @param drd
     *            The available data
     * @param iD
     *            The download id
     */
    private void fireNewDataAvailable(DownloadRequestData drd) {
        for (RadioDataManagerListener l : listeners) {
            l.newDataAvailable(drd);
        }
    }

    /**
     * Informs the radio data managers listeners about data that was removed.
     *
     * @param drd
     *            The data that was removed
     */
    private void fireDownloadRequestDataRemoved(DownloadRequestData drd) {
        for (RadioDataManagerListener l : listeners) {
            l.downloadRequestDataRemoved(drd);
        }
    }

    /**
     * Informs the radio data manager listeners about the data visibility that
     * was changed.
     *
     * @param drd
     *            The download request data for which the data visibility was
     *            changed.
     */
    private void fireDownloadRequestDataVisibilityChanged(DownloadRequestData drd) {
        for (RadioDataManagerListener l : listeners) {
            l.downloadRequestDataVisibilityChanged(drd);
        }
        lineDataSelectorModel.lineDataElementUpdated(drd);
    }

    /**
     * Informs the radio data manager listeners about the download request that
     * was answered.
     *
     * @param timeInterval
     *            The time interval over which the request was answered
     * @param downloadID
     *            The download id for which the request was answered
     */
    private void fireDownloadRequestAnswered(Interval<Date> timeInterval) {
        for (RadioDataManagerListener l : listeners) {
            l.downloadRequestAnswered(timeInterval);
        }
    }

    /**
     * Function handles a request configuration.
     *
     * @param requestConfig
     *            The request configuration to be handled
     * @param xStart
     *            The start date of the currently visible time interval
     * @param xEnd
     *            The end date of the currently visible time interval
     * @param yStart
     *            The start value of the currently visible frequency interval
     * @param yEnd
     *            The end value of the currently visible frequency interval
     */
    private void handleRequestConfig(RequestConfig requestConfig, Date xStart, Date xEnd, double yStart, double yEnd) {
        if (requestConfig.getxEnd().getTime() - requestConfig.getxStart().getTime() > EVESettings.MAXIMUM_INTERVAL_RANGE_MILLI_SEC_REQ) {
            fireIntervalTooBig();
        } else {
            RadioImageCacheResult result = cache.getRadioImageCacheResultForInterval(requestConfig.getxStart(), requestConfig.getxEnd(), 24L * 60 * 60 * 1000);
            if (downloadRequestData != null) {
                downloader.requestAndOpenIntervals(result.getMissingInterval(), requestConfig.getxRatio(), requestConfig.getyRatio());
            } else {
                Log.trace("drd is null");
            }

            if (downloadRequestData != null) {
                fireClearSavedImages(result.getToRemove());
                // Log.trace("Size of available images : " +
                // result.getAvailableData().size());
                for (DownloadedJPXData jpxData : result.getAvailableData()) {
                    handleAvailableData(jpxData, xStart, xEnd, yStart, yEnd, downloadRequestData);
                }
            }
        }

    }

    /**
     * Handles new available data send by the radio downloader
     *
     * @param xStart
     *            The start time of the currently visible time interval
     * @param xEnd
     *            The end time of the currently visible time interval
     * @param yStart
     *            The start frequency of the currently visible frequency
     *            interval
     * @param yEnd
     *            The end frequency of the currently visible frequency interval
     * @param jpxData
     *            The downloaded jpx data that should be handled
     * @param plotIdentifier
     *            The identifier of the plot
     * @param drd
     *            The download request data
     *
     */
    private void handleAvailableData(DownloadedJPXData jpxData, Date xStart, Date xEnd, double yStart, double yEnd, DownloadRequestData drd) {
        RadioImage ri = drd.getRadioImages().get(jpxData.getImageID());
        if (ri != null) {
            ri.setVisibleIntervals(xStart, xEnd, (int) Math.floor(yStart), (int) Math.ceil(yEnd));
            if (ri.getVisibleImageFreqInterval() != null && ri.getVisibleImageTimeInterval() != null) {
                Interval<Date> visibleDateInterval = ri.getVisibleImageTimeInterval();
                FrequencyInterval visibleFrequencyInterval = ri.getVisibleImageFreqInterval();
                if (!visibleDateInterval.getStart().equals(visibleDateInterval.getEnd())) {
                    JP2ViewCallisto jp2View = jpxData.getView();
                    if (jp2View != null) {
                        JP2ImageCallisto image = jp2View.getJP2Image();

                        image.setViewport(zoomManager.getAvailableSpaceForInterval(visibleDateInterval.getStart(), visibleDateInterval.getEnd(), visibleFrequencyInterval.getStart(), visibleFrequencyInterval.getEnd()));
                        image.setRegion(ri.getROI());
                        jp2View.render(null, null, 1);
                    }
                }
            }
        }
    }

    /**
     * Instructs the radio data manager listeners to clear the saved images in
     * the list.
     *
     * @param downloadID
     *            The download identifier for which the images should be removed
     * @param toRemove
     *            The list of image identifiers that should be removed
     */
    private void fireClearSavedImages(List<Long> toRemove) {
        for (long imageID : toRemove) {
            for (RadioDataManagerListener l : listeners) {
                l.clearAllSavedImagesForID(imageID);
            }
        }
    }

    /**
     * Informs the radio data manager listeners about a request for a too big
     * interval
     *
     * @param downloadID
     *            The download identifier for which the requested interval was
     *            too big
     */
    private void fireIntervalTooBig() {
        for (RadioDataManagerListener l : listeners) {
            l.intervalTooBig();
        }
    }

    /**
     * Handles newly downloaded jpx-data.
     *
     * @param djd
     *            The jpx-data to be handled
     * @param drd
     *            The corresponding download request data
     * @param downloadID
     *            The download identifier for which the data was downloaded
     */
    private void handleDownloadedJPXData(DownloadedJPXData djd, DownloadRequestData drd, double ratioX, double ratioY) {
        JP2ViewCallisto jp2View = djd.getView();
        if (jp2View != null) {
            JP2ImageCallisto image = jp2View.getJP2Image();
            image.setReaderMode(ReaderMode.ONLYFIREONCOMPLETE);
            ResolutionSet rs = image.getResolutionSet(0);
            int maximumFrameNumber = image.getMaximumFrameNumber();
            LineDataSelectorModel.getSingletonInstance().downloadStarted(drd);

            XMLMetaDataContainer hvMetaData = new XMLMetaDataContainer();
            for (int i = 0; i <= maximumFrameNumber; i++) {
                try {
                    hvMetaData.parseXML(image.getXML(i));
                    double freqStart = hvMetaData.tryGetDouble("STARTFRQ");
                    double freqEnd = hvMetaData.tryGetDouble("END-FREQ");
                    Date start = JHVDate.parseDateTime(hvMetaData.get("DATE-OBS")).getDate();
                    Date end = JHVDate.parseDateTime(hvMetaData.get("DATE-END")).getDate();
                    hvMetaData.destroyXML();

                    FrequencyInterval fi = new FrequencyInterval((int) Math.round(freqStart), (int) Math.round(freqEnd));

                    List<ResolutionSetting> resolutionSettings = new ArrayList<ResolutionSetting>();
                    if (start != null && end != null) {
                        Interval<Date> dateInterval = new Interval<Date>(start, end);
                        for (int j = 0; j <= rs.getMaxResolutionLevels(); j++) {
                            ResolutionLevel res = rs.getResolutionLevel(j);
                            ResolutionSetting tempResSet = new ResolutionSetting((end.getTime() - start.getTime()) / (double) res.width, (freqEnd - freqStart) / res.height, j, res.width, res.height, res.discardLayers);
                            resolutionSettings.add(tempResSet);
                        }

                        RadioImage tempRs = new RadioImage(djd.getImageID(), dateInterval, fi, i, rs, resolutionSettings, true);

                        ResolutionSetting lastUsedResolutionSetting = null;
                        if (Double.isNaN(ratioX) && Double.isNaN(ratioY)) {
                            int highestLevel = -1;
                            for (ResolutionSetting rst : resolutionSettings) {
                                if (rst.getResolutionLevel() > highestLevel) {
                                    highestLevel = rst.getResolutionLevel();
                                    lastUsedResolutionSetting = rst;
                                }
                            }
                        } else {
                            lastUsedResolutionSetting = tempRs.defineBestResolutionSetting(ratioX, ratioY);
                        }
                        image.setViewport(lastUsedResolutionSetting.getRectangleRepresentation());

                        tempRs.setLastUsedResolutionSetting(lastUsedResolutionSetting);
                        image.setRegion(tempRs.getROI());
                        jp2View.render(null, null, 1);

                        drd.addRadioImage(tempRs);
                    } else {
                        Log.error("Start and/or stop is null");
                    }
                } catch (Exception e) {
                    Log.error("Some of the metadata could not be read, aborting...");
                    return;
                }
            }
        }
    }

    /**
     * Informs radio data manager listeners about interval with no data that
     * where receive.
     *
     * @param noDataList
     *            A list with interval for which no data was received
     * @param downloadID
     *            The download identifier for which intervals with no data was
     *            received
     * @param plotIdentifier
     *            The plot identifier for which intervals with no data were
     *            received
     */
    private void fireNoDataIntervalsReceived(List<Interval<Date>> noDataList) {
        for (RadioDataManagerListener l : listeners) {
            l.noDataInterval(noDataList);
        }
    }

    @Override
    public void noDataInDownloadInterval(Interval<Date> requestInterval) {
        downloadRequestData = new DownloadRequestData();
        lineDataSelectorModel.addLineData(downloadRequestData);
        fireNewDataAvailable(downloadRequestData);
        fireDownloadRequestAnswered(requestInterval);
    }

}
