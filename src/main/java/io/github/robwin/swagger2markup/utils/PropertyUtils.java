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
package io.github.robwin.swagger2markup.utils;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.join;

import java.util.List;
import java.util.Objects;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Validate;

import io.github.robwin.markup.builder.MarkupLanguage;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.BooleanProperty;
import io.swagger.models.properties.DoubleProperty;
import io.swagger.models.properties.FloatProperty;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.LongProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.models.properties.UUIDProperty;

public final class PropertyUtils {

    /**
     * Retrieves the type and format of a property.
     *
     * @param property the property
     * @param markupLanguage the markup language which is used to generate the files
     * @return the type of the property
     */
    public static String getType(Property property, MarkupLanguage markupLanguage){
        Validate.notNull(property, "property must not be null!");
        
        String type;
      
        if(property instanceof RefProperty){
            RefProperty refProperty = (RefProperty)property;
            switch (markupLanguage){
                case ASCIIDOC: return "<<" + refProperty.getSimpleRef() + ">>";
                default: return refProperty.getSimpleRef();
            }
        }else if(property instanceof ArrayProperty){
            ArrayProperty arrayProperty = (ArrayProperty)property;
            Property items = arrayProperty.getItems();
            type = getType(items, markupLanguage) + " " + arrayProperty.getType();
        }else if(property instanceof StringProperty){
            StringProperty stringProperty = (StringProperty)property;
            List<String> enums = stringProperty.getEnum();
            if(CollectionUtils.isNotEmpty(enums)){
                type = "enum" + " (" + join(enums, ", ") + ")";
            }else{
                if(isNotBlank(property.getFormat())){
                    type = defaultString(property.getType()) + renderFormat(property.getFormat());
                }else{
                    type = property.getType();
                }
                if (stringProperty.getMaxLength()!=null){
                    type=type+" (max: "+stringProperty.getMaxLength()+")";
                }
                
            }
        }
        else{
            if(isNotBlank(property.getFormat())){
                type = defaultString(property.getType()) + renderFormat(property.getFormat());
            }else{
                type = property.getType();
            }
        }
        return defaultString(type);
    }

    /**
     * Render the extended format information.
     *
     * @param propFormat the format property
     * @return the formated string.
     */
    private static String renderFormat(String propFormat)
    {
        return System.lineSeparator()+"(" + propFormat + ")";      
    }

    /**
     * Retrieves the default value of a property, or otherwise returns an empty String.
     *
     * @param property the property
     * @return the default value of the property, or otherwise an empty String
     */
    public static String getDefaultValue(Property property){
        Validate.notNull(property, "property must not be null!");
        String defaultValue = "";
        if(property instanceof BooleanProperty){
            BooleanProperty booleanProperty = (BooleanProperty)property;
            defaultValue = Objects.toString(booleanProperty.getDefault(), "");
        }else if(property instanceof StringProperty){
            StringProperty stringProperty = (StringProperty)property;
            defaultValue = Objects.toString(stringProperty.getDefault(), "");
        }else if(property instanceof DoubleProperty){
            DoubleProperty doubleProperty = (DoubleProperty)property;
            defaultValue = Objects.toString(doubleProperty.getDefault(), "");
        }else if(property instanceof FloatProperty){
            FloatProperty floatProperty = (FloatProperty)property;
            defaultValue = Objects.toString(floatProperty.getDefault(), "");
        }else if(property instanceof IntegerProperty){
            IntegerProperty integerProperty = (IntegerProperty)property;
            defaultValue = Objects.toString(integerProperty.getDefault(), "");
        }
        else if(property instanceof LongProperty){
            LongProperty longProperty = (LongProperty)property;
            defaultValue = Objects.toString(longProperty.getDefault(), "");
        }
        else if(property instanceof UUIDProperty){
            UUIDProperty uuidProperty = (UUIDProperty)property;
            defaultValue = Objects.toString(uuidProperty.getDefault(), "");
        }
        return defaultValue;
    }
}
