package com.inkwell.common;

/** Published after a catalog item or post is saved; triggers asynchronous embedding. */
public record ContentChangedEvent(String table, long id) {}
