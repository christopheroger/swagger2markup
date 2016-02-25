/*
 *
 *  Copyright 2015 Robert Winkler
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.robwin.swagger2markup.builder.document;

import com.google.common.collect.Ordering;
import io.github.robwin.markup.builder.MarkupDocBuilder;
import io.github.robwin.markup.builder.MarkupDocBuilders;
import io.github.robwin.markup.builder.MarkupLanguage;
import io.github.robwin.markup.builder.MarkupTableColumn;
import io.github.robwin.swagger2markup.config.Swagger2MarkupConfig;
import io.github.robwin.swagger2markup.type.DefinitionDocumentResolver;
import io.github.robwin.swagger2markup.type.ObjectType;
import io.github.robwin.swagger2markup.type.RefType;
import io.github.robwin.swagger2markup.type.Type;
import io.github.robwin.swagger2markup.utils.PropertyUtils;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.defaultString;

/**
 * @author Robert Winkler
 */
public abstract class MarkupDocument {

    protected static final Pattern FILENAME_FORBIDDEN_PATTERN = Pattern.compile("[^0-9A-Za-z-_]+");

    protected final String DEFAULT_COLUMN;
    protected final String REQUIRED_COLUMN;
    protected final String SCHEMA_COLUMN;
    protected final String NAME_COLUMN;
    protected final String DESCRIPTION_COLUMN;

    protected final String SCOPES_COLUMN;
    protected final String DESCRIPTION;
    protected final String PRODUCES;
    protected final String CONSUMES;
    protected final String TAGS;
    protected final String NO_CONTENT;
    protected Logger logger = LoggerFactory.getLogger(getClass());
    protected Swagger swagger;
    protected MarkupLanguage markupLanguage;
    protected MarkupDocBuilder markupDocBuilder;
    protected boolean separatedDefinitionsEnabled;
    protected String separatedDefinitionsFolder;
    protected String definitionsDocument;
    protected String outputDirectory;
    protected boolean useInterDocumentCrossReferences;
    protected String interDocumentCrossReferencesPrefix;
    protected Comparator<String> propertyOrdering;


    MarkupDocument(Swagger2MarkupConfig swagger2MarkupConfig, String outputDirectory) {
        this.swagger = swagger2MarkupConfig.getSwagger();
        this.markupLanguage = swagger2MarkupConfig.getMarkupLanguage();
        this.markupDocBuilder = MarkupDocBuilders.documentBuilder(markupLanguage).withAnchorPrefix(swagger2MarkupConfig.getAnchorPrefix());
        this.separatedDefinitionsEnabled = swagger2MarkupConfig.isSeparatedDefinitions();
        this.separatedDefinitionsFolder = swagger2MarkupConfig.getSeparatedDefinitionsFolder();
        this.definitionsDocument = swagger2MarkupConfig.getDefinitionsDocument();
        this.outputDirectory = outputDirectory;
        this.useInterDocumentCrossReferences = swagger2MarkupConfig.isInterDocumentCrossReferences();
        this.interDocumentCrossReferencesPrefix = swagger2MarkupConfig.getInterDocumentCrossReferencesPrefix();
        this.propertyOrdering = swagger2MarkupConfig.getPropertyOrdering();

        ResourceBundle labels = ResourceBundle.getBundle("lang/labels",
                swagger2MarkupConfig.getOutputLanguage());
        DEFAULT_COLUMN = labels.getString("default_column");
        REQUIRED_COLUMN = labels.getString("required_column");
        SCHEMA_COLUMN = labels.getString("schema_column");
        NAME_COLUMN = labels.getString("name_column");
        DESCRIPTION_COLUMN = labels.getString("description_column");
        SCOPES_COLUMN = labels.getString("scopes_column");
        DESCRIPTION = DESCRIPTION_COLUMN;
        PRODUCES = labels.getString("produces");
        CONSUMES = labels.getString("consumes");
        TAGS = labels.getString("tags");
        NO_CONTENT = labels.getString("no_content");
    }

    /**
     * Builds the MarkupDocument.
     *
     * @return the built MarkupDocument
     * @throws IOException if the files to include are not readable
     */
    public abstract MarkupDocument build() throws IOException;

    /**
     * Returns a string representation of the document.
     */
    public String toString() {
        return markupDocBuilder.toString();
    }

    /**
     * Writes the content of the builder to a file and clears the builder.
     *
     * @param directory the directory where the generated file should be stored
     * @param fileName  the name of the file
     * @param charset   the the charset to use for encoding
     * @throws IOException if the file cannot be written
     */
    public void writeToFile(String directory, String fileName, Charset charset) throws IOException {
        markupDocBuilder.writeToFile(directory, fileName, charset);
    }

    /**
     * Create a normalized filename
     * @param name current name of the file
     * @return a normalized filename
     */
    protected String normalizeFileName(String name) {
        String fileName = FILENAME_FORBIDDEN_PATTERN.matcher(name).replaceAll("_");
        fileName = fileName.replaceAll(String.format("([%1$s])([%1$s]+)", "-_"), "$1");
        fileName = StringUtils.strip(fileName, "_-");
        fileName = fileName.trim().toLowerCase();
        return fileName;
    }

    /**
     * Build a generic property table for any ObjectType
     * @param type to display
     * @param uniquePrefix unique prefix to prepend to inline object names to enforce unicity
     * @param depth current inline schema object depth
     * @param propertyDescriptor property descriptor to apply to properties
     * @param definitionDocumentResolver definition document resolver to apply to property type cross-reference
     * @param docBuilder the docbuilder do use for output
     * @return a list of inline schemas referenced by some properties, for later display
     */
    public List<ObjectType> typeProperties(ObjectType type, String uniquePrefix, int depth, PropertyDescriptor propertyDescriptor, DefinitionDocumentResolver definitionDocumentResolver, MarkupDocBuilder docBuilder) {
        List<ObjectType> localDefinitions = new ArrayList<>();
        List<List<String>> cells = new ArrayList<>();
        List<MarkupTableColumn> cols = Arrays.asList(
                new MarkupTableColumn(NAME_COLUMN, 1).withMarkupSpecifiers(MarkupLanguage.ASCIIDOC, ".^1h"),
                new MarkupTableColumn(DESCRIPTION_COLUMN, 6).withMarkupSpecifiers(MarkupLanguage.ASCIIDOC, ".^6"),
                new MarkupTableColumn(REQUIRED_COLUMN, 1).withMarkupSpecifiers(MarkupLanguage.ASCIIDOC, ".^1"),
                new MarkupTableColumn(SCHEMA_COLUMN, 1).withMarkupSpecifiers(MarkupLanguage.ASCIIDOC, ".^1"),
                new MarkupTableColumn(DEFAULT_COLUMN, 1).withMarkupSpecifiers(MarkupLanguage.ASCIIDOC, ".^1"));
        if (MapUtils.isNotEmpty(type.getProperties())) {
            Set<String> propertyNames;
            if (this.propertyOrdering == null)
                propertyNames = new LinkedHashSet<>();
            else
                propertyNames = new TreeSet<>(this.propertyOrdering);
            propertyNames.addAll(type.getProperties().keySet());

            for (String propertyName: propertyNames) {
                Property property = type.getProperties().get(propertyName);
                Type propertyType = PropertyUtils.getType(property, definitionDocumentResolver);
                if (depth > 0 && propertyType instanceof ObjectType) {
                    if (MapUtils.isNotEmpty(((ObjectType) propertyType).getProperties())) {
                        propertyType.setName(propertyName);
                        propertyType.setUniqueName(uniquePrefix + " " + propertyName);
                        localDefinitions.add((ObjectType)propertyType);

                        propertyType = new RefType(propertyType);
                    }
                }

                List<String> content = Arrays.asList(
                        propertyName,
                        propertyDescriptor.getDescription(property, propertyName),
                        Boolean.toString(property.getRequired()),
                        propertyType.displaySchema(docBuilder),
                        PropertyUtils.getDefaultValue(property));
                cells.add(content);
            }
            docBuilder.tableWithColumnSpecs(cols, cells);
        } else {
            docBuilder.textLine(NO_CONTENT);
        }

        return localDefinitions;
    }

    /**
     * A functor to return descriptions for a given property
     */
    public class PropertyDescriptor {
        protected Type type;

        public PropertyDescriptor(Type type) {
            this.type = type;
        }

        public String getDescription(Property property, String propertyName) {
            return defaultString(property.getDescription());
        }
    }

    /**
     * Default {@code DefinitionDocumentResolver} functor
     */
    class DefinitionDocumentResolverDefault implements DefinitionDocumentResolver {

        public DefinitionDocumentResolverDefault() {}

        public String apply(String definitionName) {
            if (!useInterDocumentCrossReferences || outputDirectory == null)
                return null;
            else if (separatedDefinitionsEnabled)
                return interDocumentCrossReferencesPrefix + new File(separatedDefinitionsFolder, markupDocBuilder.addfileExtension(normalizeFileName(definitionName))).getPath();
            else
                return interDocumentCrossReferencesPrefix + markupDocBuilder.addfileExtension(definitionsDocument);
        }
    }
}
