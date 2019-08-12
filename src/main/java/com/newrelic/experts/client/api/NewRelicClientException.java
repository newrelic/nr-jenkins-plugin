/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0 
 */

package com.newrelic.experts.client.api;

import java.io.IOException;

/**
 * An {@link IOException} sub-class for flagging exceptions thrown by a
 * {@link NewRelicClient}.
 * 
 * @author Scott DeWitt (sdewitt@newrelic.com)
 */
public class NewRelicClientException extends IOException {

  /**
   * Default serial version UID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructs an {@code NewRelicClientException} with the specified
   * detail message.
   *
   * @param message
   *        The detail message (which is saved for later retrieval
   *        by the {@link #getMessage()} method)
   */
  public NewRelicClientException(String message) {
    super(message);
  }

  /**
   * Constructs an {@code NewRelicClientException} with the specified cause and
   * a detail message of {@code (cause==null ? null : cause.toString())}
   * (which typically contains the class and detail message of {@code cause}).
   * This constructor is useful for IO exceptions that are little more
   * than wrappers for other throwables.
   *
   * @param cause
   *        The cause (which is saved for later retrieval by the
   *        {@link #getCause()} method).  (A null value is permitted,
   *        and indicates that the cause is nonexistent or unknown.)
   *
   * @since 1.6
   */
  public NewRelicClientException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs an {@code NewRelicClientException} with the specified detail 
   * message and cause.
   *
   * <p>
   * Note that the detail message associated with {@code cause} is
   * <i>not</i> automatically incorporated into this exception's detail
   * message.
   * </p>
   *
   * @param message
   *        The detail message (which is saved for later retrieval
   *        by the {@link #getMessage()} method)
   *
   * @param cause
   *        The cause (which is saved for later retrieval by the
   *        {@link #getCause()} method).  (A null value is permitted,
   *        and indicates that the cause is nonexistent or unknown.)
   *
   * @since 1.6
   */
  public NewRelicClientException(String message, Throwable cause) {
    super(message, cause);
  }

}
