package io.github.defective4.har4zip.codec.model;

public record HttpArchiveInfo(BrowserInfo creator, BrowserInfo browser, PageInfo[] pages) {
    @Override
    public PageInfo[] pages() {
        return pages == null ? new PageInfo[0] : pages;
    }
}
