/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.main;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Mike Mirzayanov
 */
class ReloadingClassLoader extends ClassLoader {
    /**
     * Standard class loader class path.
     */
    private static final URL[] classPathUrls = ((URLClassLoader) ReloadingClassLoader.class.getClassLoader()).getURLs();

    /**
     * Class loader for delegation.
     */
    private final DelegationClassLoader delegationClassLoader;

    /**
     * Creates new instance of ReloadingClassLoader.
     */
    public ReloadingClassLoader() {
        List<URL> delegationClassLoaderClassPath = new ArrayList<>();
        List<File> classPathItems = ReloadingContext.getInstance().getReloadingClassPaths();

        for (File classPathDir : classPathItems) {
            if (classPathDir.isDirectory()) {
                try {
                    delegationClassLoaderClassPath.add(classPathDir.toURI().toURL());
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException("The path " + classPathDir.getAbsolutePath() + " is not valid URL.", e);
                }
            } else {
                throw new IllegalArgumentException("Expected to find directory for the path " + classPathDir.getName() + '.');
            }
        }

        delegationClassLoaderClassPath.addAll(Arrays.asList(classPathUrls));

        delegationClassLoader = new DelegationClassLoader(delegationClassLoaderClassPath.toArray(
                new URL[delegationClassLoaderClassPath.size()]));
    }

    /**
     * @return Delegation classLoader which actually loads classes to be hot-swapped.
     */
    DelegationClassLoader getDelegationClassLoader() {
        return delegationClassLoader;
    }

    /**
     * Loads class.
     *
     * @param name    Class name.
     * @param resolve Resolve.
     * @return Class Loaded class.
     */
    @Override
    public Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return delegationClassLoader.loadClass(name, resolve);
    }

    /**
     * Load class using standard loader.
     *
     * @param name    Class name.
     * @param resolve Resolved.
     * @return Class Loaded class.
     * @throws ClassNotFoundException when Can't load class.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    private static Class loadUsingStandardClassLoader(String name, boolean resolve) throws ClassNotFoundException {
        return Thread.currentThread().getContextClassLoader().loadClass(name);
    }

    /**
     * @param name Class name.
     * @return boolean {@code true} iff this class should be loaded by standard class.
     */
    private static boolean isForceToLoadUsingStandardClassLoader(String name) {
        boolean reload = false;

        String nameWithDot = name + '.';
        String nameWithDollar = name + '$';

        // Check if it is in the reloading packages.
        List<String> classReloadingPackages = ReloadingContext.getInstance().getClassReloadingPackages();
        for (String classReloadingPackage : classReloadingPackages) {
            if (nameWithDot.startsWith(classReloadingPackage + '.')) {
                reload = true;
                break;
            }
        }

        // Check if it is in exceptions.
        if (reload) {
            List<String> exceptions = new LinkedList<>(ReloadingContext.getInstance().getClassReloadingExceptions());
            for (String exception : exceptions) {
                if (nameWithDot.startsWith(exception + '.') || nameWithDollar.startsWith(exception + '$')) {
                    reload = false;
                    break;
                }
            }
        }

        return !reload;
    }

    class DelegationClassLoader extends URLClassLoader {
        public DelegationClassLoader(URL[] urls) {
            super(urls);
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            // Use standard class loader?
            if (isForceToLoadUsingStandardClassLoader(name)) {
                return loadUsingStandardClassLoader(name, resolve);
            }

            return super.loadClass(name, resolve);
        }
    }
}
