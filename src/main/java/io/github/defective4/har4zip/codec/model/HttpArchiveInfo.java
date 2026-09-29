package io.github.defective4.har4zip.codec.model;

public record HttpArchiveInfo(BrowserInfo creator, BrowserInfo browser, PageInfo[] pages) {
}
