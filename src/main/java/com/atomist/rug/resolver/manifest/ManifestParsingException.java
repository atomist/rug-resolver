package com.atomist.rug.resolver.manifest;

public class ManifestParsingException extends RuntimeException {

    private static final long serialVersionUID = -5616066311972886869L;

    public ManifestParsingException(String msg, Exception e) {
        super(msg, e);
    }

    public ManifestParsingException(String msg, String... tokens) {
        super(String.format(msg, (Object[]) tokens));
    }
}
