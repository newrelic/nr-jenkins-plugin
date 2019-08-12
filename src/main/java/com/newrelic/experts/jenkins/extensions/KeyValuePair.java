/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.jenkins.extensions;

import com.newrelic.experts.jenkins.Messages;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * A {@link Describable} that represents a simple key/value pair that can be
 * used to add custom event attributes.
 * 
 * @author Scott DeWitt (sdewitt@newrelic.com)
 */
@ExportedBean
public class KeyValuePair extends AbstractDescribableImpl<KeyValuePair> {

  private String name;
  private String value;
  
  @DataBoundConstructor
  public KeyValuePair(
      String name,
      String value
  ) {
    this.name = name;
    this.value = value;
  }

  @Exported
  public String getName() {
    return this.name;
  }

  @Exported
  public String getValue() {
    return this.value;
  }
  
  /**
   * The {@link Descriptor} class for the {@link KeyValuePair}.
   * 
   * @author Scott DeWitt (sdewitt@newrelic.com)
   */
  @Extension
  public static final class KeyValuePairDescriptorImpl extends Descriptor<KeyValuePair> {

    @Override
    public String getDisplayName() {
      return Messages.KeyValuePair_DisplayName();
    }
    
    /**
     * Validate that the key (name) field is not empty, properly formatted,
     * and does not appear to be a token expression.
     * 
     * @param name the current value of the key/name field.
     * @return a {@link FormValidation} result.
     */
    public FormValidation doCheckName(
        @QueryParameter String name
    ) {
      if (StringUtils.isBlank(name)) {
        return FormValidation.error(
            Messages.KeyValuePair_errors_EmptyName()
        );
      }
      if (name.startsWith("${")
          && name.endsWith("}")) {
        return FormValidation.error(
            Messages.KeyValuePair_errors_NameExpressionNotSupported()
        );
      }
      if (!name.matches("[a-zA-Z0-9_.]+")) {
        return FormValidation.error(
            Messages.KeyValuePair_errors_NameSyntaxInvalid()
        );
      }
      return FormValidation.ok();
    }
    
    /**
     * Validate that the value fields is not empty.
     * 
     * @param value the current value of the value field.
     * @return a {@link FormValidation} result.
     */
    public FormValidation doCheckValue(
        @QueryParameter String value
    ) {
      if (StringUtils.isBlank(value)) {
        return FormValidation.error(
            Messages.KeyValuePair_errors_EmptyValue()
        );
      }
      return FormValidation.ok();
    }
  }

}
