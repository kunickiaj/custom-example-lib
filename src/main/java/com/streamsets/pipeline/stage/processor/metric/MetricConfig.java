/*
 * Copyright 2017 StreamSets Inc.
 *
 * Licensed under the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamsets.pipeline.stage.processor.metric;

import com.streamsets.pipeline.api.ConfigDef;
import com.streamsets.pipeline.api.ValueChooserModel;
import com.streamsets.pipeline.lib.el.RecordEL;

public class MetricConfig {
  @ConfigDef(
      required = true,
      type = ConfigDef.Type.STRING,
      label = "Name",
      description = "Evaluated expression is the metric name.",
      evaluation = ConfigDef.Evaluation.EXPLICIT,
      elDefs = RecordEL.class,
      displayPosition = 10,
      group = "#0"
  )
  public String name;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.STRING,
      label = "Expression",
      description = "Boolean expression determines whether metric is incremented.",
      evaluation = ConfigDef.Evaluation.EXPLICIT,
      elDefs = RecordEL.class,
      displayPosition = 10,
      group = "#0"
  )
  public String condition;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.MODEL,
      defaultValue = "METER",
      label = "Type",
      displayPosition = 20,
      group = "#0"
  )
  @ValueChooserModel(MetricTypeChooserValues.class)
  public MetricType type;
}
