package io.github.lc.oss.commons.web.controllers;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;

import io.github.lc.oss.commons.util.PathNormalizer;
import io.github.lc.oss.commons.web.resources.StaticResourceFileResolver;

public class ThemeResourceFileResolver extends StaticResourceFileResolver {
    @Autowired
    private PathNormalizer pathNormalizer;
    @Autowired(required = false)
    private UserTheme userTheme;

    public ThemeResourceFileResolver(String root, int depth) {
        super(root, depth);
    }

    @Override
    public String getRoot() {
        return this.getThemesRoot() + this.getPathNormalizer().dir(this.getTheme());
    }

    @Override
    public List<String> findFiles(Types type, Predicate<Path> additionalPathMatcher) {
        if (this.getTheme() == null) {
            return null;
        }
        return super.findFiles(type, additionalPathMatcher);
    }

    public String getThemesRoot() {
        return super.getRoot();
    }

    private PathNormalizer getPathNormalizer() {
        return this.pathNormalizer;
    }

    private String getTheme() {
        if (this.getUserTheme() == null) {
            return null;
        }
        return this.getUserTheme().getName();
    }

    private UserTheme getUserTheme() {
        return this.userTheme;
    }
}
