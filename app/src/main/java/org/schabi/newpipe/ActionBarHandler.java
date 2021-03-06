package org.schabi.newpipe;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import org.schabi.newpipe.services.MediaFormat;
import org.schabi.newpipe.services.VideoInfo;

/**
 * Created by Christian Schabesberger on 18.08.15.
 *
 * Copyright (C) Christian Schabesberger 2015 <chris.schabesberger@mailbox.org>
 * DetailsMenuHandler.java is part of NewPipe.
 *
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */


class ActionBarHandler {
    private static final String TAG = ActionBarHandler.class.toString();
    private static final String KORE_PACKET = "org.xbmc.kore";

    private int serviceId;
    private String websiteUrl = "";
    private Bitmap videoThumbnail = null;
    private String channelName = "";
    private AppCompatActivity activity;
    private VideoInfo.VideoStream[] videoStreams = null;
    private VideoInfo.AudioStream audioStream = null;
    private int selectedStream = -1;
    private String videoTitle = "";

    private SharedPreferences defaultPreferences = null;
    private int startPosition;

    @SuppressWarnings("deprecation")
    private class FormatItemSelectListener implements ActionBar.OnNavigationListener {
        @Override
        public boolean onNavigationItemSelected(int itemPosition, long itemId) {
            selectFormatItem((int)itemId);
            return true;
        }
    }

    public ActionBarHandler(AppCompatActivity activity) {
        this.activity = activity;
    }

    @SuppressWarnings({"deprecation", "ConstantConditions"})
    public void setupNavMenu(AppCompatActivity activity) {
        this.activity = activity;
        try {
            activity.getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        } catch(NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void setServiceId(int id) {
        serviceId = id;
    }

    public void setSetVideoThumbnail(Bitmap bitmap) {
        videoThumbnail = bitmap;
    }

    public void setChannelName(String name) {
        channelName = name;
    }

    @SuppressWarnings("deprecation")
    public void setStreams(VideoInfo.VideoStream[] videoStreams, VideoInfo.AudioStream[] audioStreams) {
        this.videoStreams = videoStreams;
        selectedStream = 0;
        defaultPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        String[] itemArray = new String[videoStreams.length];
        String defaultResolution = defaultPreferences
                .getString(activity.getString(R.string.default_resolution_key),
                        activity.getString(R.string.default_resolution_value));
        int defaultResolutionPos = 0;

        for(int i = 0; i < videoStreams.length; i++) {
            itemArray[i] = MediaFormat.getNameById(videoStreams[i].format) + " " + videoStreams[i].resolution;
            if(defaultResolution.equals(videoStreams[i].resolution)) {
                defaultResolutionPos = i;
            }
        }

        ArrayAdapter<String> itemAdapter = new ArrayAdapter<>(activity.getBaseContext(),
                android.R.layout.simple_spinner_dropdown_item, itemArray);
        if(activity != null) {
            ActionBar ab = activity.getSupportActionBar();
                assert ab != null : "Could not get actionbar";
                ab.setListNavigationCallbacks(itemAdapter
                        , new FormatItemSelectListener());

            ab.setSelectedNavigationItem(defaultResolutionPos);
        }

        // set audioStream
        audioStream = null;
        String preferedFormat = defaultPreferences
                .getString(activity.getString(R.string.default_audio_format_key), "webm");
        if(preferedFormat.equals("webm")) {
            for(VideoInfo.AudioStream s : audioStreams) {
                if(s.format == MediaFormat.WEBMA.id) {
                    audioStream = s;
                }
            }
        } else if(preferedFormat.equals("m4a")){
            for(VideoInfo.AudioStream s : audioStreams) {
                if(s.format == MediaFormat.M4A.id &&
                        (audioStream == null || audioStream.bandwidth > s.bandwidth)) {
                    audioStream = s;
                }
            }
        }
        else {
            Log.e(TAG, "FAILED to set audioStream value!");
        }
    }

    private void selectFormatItem(int i) {
        selectedStream = i;
    }

    public void setupMenu(Menu menu, MenuInflater inflater) {
        // CAUTION set item properties programmatically otherwise it would not be accepted by
        // appcompat itemsinflater.inflate(R.menu.videoitem_detail, menu);

        defaultPreferences = PreferenceManager.getDefaultSharedPreferences(activity);

        inflater.inflate(R.menu.videoitem_detail, menu);
        MenuItem castItem = menu.findItem(R.id.action_play_with_kodi);

        castItem.setVisible(defaultPreferences
                .getBoolean(activity.getString(R.string.show_play_with_kodi_key), false));
    }

    public boolean onItemSelected(MenuItem item) {
        if(!videoTitle.isEmpty()) {
            int id = item.getItemId();
            switch (id) {
                case R.id.menu_item_share: {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_SEND);
                    intent.putExtra(Intent.EXTRA_TEXT, websiteUrl);
                    intent.setType("text/plain");
                    activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.share_dialog_title)));
                    return true;
                }
                case R.id.menu_item_openInBrowser: {
                    openInBrowser();
                }
                return true;
                case R.id.menu_item_download:
                    downloadVideo();
                    return true;
                case R.id.action_settings: {
                    Intent intent = new Intent(activity, SettingsActivity.class);
                    activity.startActivity(intent);
                }
                break;
                case R.id.action_play_with_kodi:
                    playWithKodi();
                    return true;
                case R.id.menu_item_play_audio:
                    playAudio();
                    return true;
                default:
                    Log.e(TAG, "Menu Item not known");
            }
        } else {
            // That line may not be necessary.
            return true;
        }
        return false;
    }

    public void setVideoInfo(String websiteUrl, String videoTitle) {
        this.websiteUrl = websiteUrl;
        this.videoTitle = videoTitle;
    }

    public void playVideo() {
        // ----------- THE MAGIC MOMENT ---------------
        if(!videoTitle.isEmpty()) {
            if (PreferenceManager.getDefaultSharedPreferences(activity)
                    .getBoolean(activity.getString(R.string.use_external_video_player_key), false)) {

                // External Player
                Intent intent = new Intent();
                try {
                    intent.setAction(Intent.ACTION_VIEW);

                    intent.setDataAndType(Uri.parse(videoStreams[selectedStream].url),
                            MediaFormat.getMimeById(videoStreams[selectedStream].format));
                    intent.putExtra(Intent.EXTRA_TITLE, videoTitle);
                    intent.putExtra("title", videoTitle);

                    activity.startActivity(intent);      // HERE !!!
                } catch (Exception e) {
                    e.printStackTrace();
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setMessage(R.string.no_player_found)
                            .setPositiveButton(R.string.install, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent();
                                    intent.setAction(Intent.ACTION_VIEW);
                                    intent.setData(Uri.parse(activity.getString(R.string.fdroid_vlc_url)));
                                    activity.startActivity(intent);
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                    builder.create().show();
                }
            } else {
                // Internal Player
                Intent intent = new Intent(activity, PlayVideoActivity.class);
                intent.putExtra(PlayVideoActivity.VIDEO_TITLE, videoTitle);
                intent.putExtra(PlayVideoActivity.STREAM_URL, videoStreams[selectedStream].url);
                intent.putExtra(PlayVideoActivity.VIDEO_URL, websiteUrl);
                intent.putExtra(PlayVideoActivity.START_POSITION, startPosition);
                activity.startActivity(intent);     //also HERE !!!
            }
        }
        // --------------------------------------------
    }

    public void setStartPosition(int startPositionSeconds)
    {
        this.startPosition = startPositionSeconds;
    }

    private void downloadVideo() {
        if(!videoTitle.isEmpty()) {
            String videoSuffix = "." + MediaFormat.getSuffixById(videoStreams[selectedStream].format);
            String audioSuffix = "." + MediaFormat.getSuffixById(audioStream.format);
            Bundle args = new Bundle();
            args.putString(DownloadDialog.FILE_SUFFIX_VIDEO, videoSuffix);
            args.putString(DownloadDialog.FILE_SUFFIX_AUDIO, audioSuffix);
            args.putString(DownloadDialog.TITLE, videoTitle);
            args.putString(DownloadDialog.VIDEO_URL, videoStreams[selectedStream].url);
            args.putString(DownloadDialog.AUDIO_URL, audioStream.url);
            DownloadDialog downloadDialog = new DownloadDialog();
            downloadDialog.setArguments(args);
            downloadDialog.show(activity.getSupportFragmentManager(), "downloadDialog");
        }
    }

    private void openInBrowser() {
        if(!videoTitle.isEmpty()) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(websiteUrl));

            activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.choose_browser)));
        }
    }

    private void playWithKodi() {
        if(!videoTitle.isEmpty()) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setPackage(KORE_PACKET);
                intent.setData(Uri.parse(websiteUrl.replace("https", "http")));
                activity.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setMessage(R.string.kore_not_found)
                        .setPositiveButton(R.string.install, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent();
                                intent.setAction(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(activity.getString(R.string.fdroid_kore_url)));
                                activity.startActivity(intent);
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });
                builder.create().show();
            }
        }
    }

    public void playAudio() {

        boolean externalAudioPlayer = PreferenceManager.getDefaultSharedPreferences(activity)
                .getBoolean(activity.getString(R.string.use_external_audio_player_key), false);
        Intent intent;

        if (!externalAudioPlayer && android.os.Build.VERSION.SDK_INT >= 18) {
            //internal music player: explicit intent
            if (!BackgroundPlayer.isRunning  && videoThumbnail != null) {
                ActivityCommunicator.getCommunicator()
                        .backgroundPlayerThumbnail = videoThumbnail;
                intent = new Intent(activity, BackgroundPlayer.class);

                intent.setAction(Intent.ACTION_VIEW);
                Log.i(TAG, "audioStream is null:" + (audioStream == null));
                Log.i(TAG, "audioStream.url is null:" + (audioStream.url == null));
                intent.setDataAndType(Uri.parse(audioStream.url),
                        MediaFormat.getMimeById(audioStream.format));
                intent.putExtra(BackgroundPlayer.TITLE, videoTitle);
                intent.putExtra(BackgroundPlayer.WEB_URL, websiteUrl);
                intent.putExtra(BackgroundPlayer.SERVICE_ID, serviceId);
                intent.putExtra(BackgroundPlayer.CHANNEL_NAME, channelName);
                activity.startService(intent);
            }
        } else {
            intent = new Intent();
            try {
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(audioStream.url),
                        MediaFormat.getMimeById(audioStream.format));
                intent.putExtra(Intent.EXTRA_TITLE, videoTitle);
                intent.putExtra("title", videoTitle);

                activity.startActivity(intent);      // HERE !!!
            } catch (Exception e) {
                e.printStackTrace();
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setMessage(R.string.no_player_found)
                        .setPositiveButton(R.string.install, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent();
                                intent.setAction(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(activity.getString(R.string.fdroid_vlc_url)));
                                activity.startActivity(intent);
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.i(TAG, "You unlocked a secret unicorn.");
                            }
                        });
                builder.create().show();
                Log.e(TAG, "Either no Streaming player for audio was installed, or something important crashed:");
                e.printStackTrace();
            }
        }
    }
}
