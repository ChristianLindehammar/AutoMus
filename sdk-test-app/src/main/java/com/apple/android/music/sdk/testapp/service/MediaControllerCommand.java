package com.apple.android.music.sdk.testapp.service;

/**
 * Copyright (C) 2017 Apple, Inc. All rights reserved.
 */
public interface MediaControllerCommand {

    String COMMAND_REMOVE_QUEUE_ITEM = "com.apple.android.music.playback.command.REMOVE_QUEUE_ITEM";
    String COMMAND_MOVE_QUEUE_ITEM = "com.apple.android.music.playback.command.MOVE_QUEUE_ITEM";
    String COMMAND_ADD_QUEUE_ITEMS = "com.apple.android.music.playback.command.ADD_QUEUE_ITEMS";

    String COMMAND_ARGUMENT_PLAYBACK_QUEUE_ID = "com.apple.android.music.playback.command.ARGUMENT_PLAYBACK_QUEUE_ID";
    String COMMAND_ARGUMENT_PLAYBACK_QUEUE_ID_TARGET = "com.apple.android.music.playback.command.ARGUMENT_PLAYBACK_QUEUE_ID_TARGET";
    String COMMAND_ARGUMENT_PLAYBACK_QUEUE_MOVE_TARGET_TYPE = "com.apple.android.music.playback.command.ARGUMENT_PLAYBACK_QUEUE_MOVE_TARGET_TYPE";
    String COMMAND_ARGUMENT_PLAYBACK_QUEUE_INSERTION_TYPE = "com.apple.android.music.playback.command.ARGUMENT_PLAYBACK_QUEUE_INSERTION_TYPE";
    String COMMAND_ARGUMENT_PLAYBACK_QUEUE_ITEM_PROVIDER = "com.apple.android.music.playback.command.ARGUMENT_PLAYBACK_QUEUE_ITEM_PROVIDER";

}
