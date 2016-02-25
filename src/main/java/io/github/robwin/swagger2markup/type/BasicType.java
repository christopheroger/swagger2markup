package io.github.robwin.swagger2markup.type;

import io.github.robwin.markup.builder.MarkupDocBuilder;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Base type abstraction (string, integer, ...)
 */
public class BasicType extends Type {

    protected String format;

    public BasicType(String name) {
        this(name, null);
    }

    public BasicType(String name, String format) {
        super(name);
        this.format = format;
    }

    @Override
    public String displaySchema(MarkupDocBuilder docBuilder) {
        if (isNotBlank(this.format))
            return this.name + "(" + this.format + ")";
        else
            return this.name;
    }
}
