/*
 * Copyright 2017 Stephan Fuhrmann.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sfuhrm.radiorecorder;

import lombok.Getter;

/**
 * Application internal exception.
 * @author Stephan Fuhrmann
 */
public class RadioException extends RuntimeException {

    /** Is the cause of the exception tagged as being retryable?
     * This is typically the case when there is a I/O problem with the
     * remote side.
     */
    @Getter
    private final boolean retryable;

    /**
     * Constructor for a RadioException.
     * @param retryable whether the problem source could only be temporarily and a retry might make sense.
     * @param cause the causing exception.
     */
    public RadioException(boolean retryable, Throwable cause) {
        super(cause);
        this.retryable = retryable;
    }
}
