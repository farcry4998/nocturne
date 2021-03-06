/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.main;

import freemarker.core.Environment;
import freemarker.template.*;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Use code like {@code <@once>your code</@once>} to be sure that it
 * included into output at most one.
 *
 * @author Mike Mirzayanov
 */
public class OnceDirective implements TemplateDirectiveModel {
    private final Set<String> done = new HashSet<>();

    OnceDirective() {
        // No operations.
    }

    @Override
    public void execute(Environment environment, Map map, TemplateModel[] templateModels, TemplateDirectiveBody body) throws TemplateException, IOException {
        if (map.size() != 1 || map.get("scope") == null) {
            throw new TemplateException("Once directive expects exactly one autogenerated attribute 'scope'.", environment);
        }

        String scope = map.get("scope").toString();
        if (!done.contains(scope)) {
            char[] chars;

            if (body != null) {
                CharArrayWriter writer = new CharArrayWriter();
                body.render(writer);
                writer.close();
                chars = writer.toCharArray();
                environment.getOut().write(chars);
                environment.getOut().flush();
            }

            done.add(scope);
        }
    }
}
