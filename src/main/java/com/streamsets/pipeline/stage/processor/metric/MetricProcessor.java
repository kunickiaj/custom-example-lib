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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.streamsets.pipeline.stage.lib.ELUtils;
import com.google.common.base.Strings;
import com.streamsets.pipeline.api.EventRecord;
import com.streamsets.pipeline.api.Field;
import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.api.base.SingleLaneRecordProcessor;
import com.streamsets.pipeline.api.el.ELEval;
import com.streamsets.pipeline.api.el.ELVars;
import com.streamsets.pipeline.api.impl.Utils;
import com.streamsets.pipeline.lib.el.FakeRecordEL;
import com.streamsets.pipeline.lib.el.RecordEL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;

public abstract class MetricProcessor extends SingleLaneRecordProcessor {

  private ELEval metricNameEval;
  private ELVars metricNameVars;

  private ELEval conditionEval;
  private ELVars conditionVars;

  private Map<String, MetricType> counterNames;

  public abstract MetricProcessorConfig getConf();

  @Override
  protected List<ConfigIssue> init() {
    List<ConfigIssue> issues = super.init();

    counterNames = new HashMap<>();

    for (MetricConfig counter : getConf().counters) {
      metricNameEval = getContext().createELEval("name", FakeRecordEL.class);
      metricNameVars = getContext().createELVars();

      conditionEval = getContext().createELEval("condition", RecordEL.class);
      conditionVars = getContext().createELVars();

      ELUtils.validateExpression(
          metricNameEval,
          metricNameVars,
          counter.name,
          getContext(),
          Groups.METRICS.name(),
          "conf.counters",
          Errors.METRICS_00,
          String.class,
          issues
      );

      ELUtils.validateExpression(
          conditionEval,
          conditionVars,
          counter.name,
          getContext(),
          Groups.METRICS.name(),
          "conf.counters",
          Errors.METRICS_00,
          String.class,
          issues
      );
    }

    metricNameEval = getContext().createELEval("name", RecordEL.class);
    metricNameVars = getContext().createELVars();

    conditionEval = getContext().createELEval("condition", RecordEL.class);
    conditionVars = getContext().createELVars();

    return issues;
  }

  @Override
  public void destroy() {
    // Emit metrics events for each counter
    for (Map.Entry<String, MetricType> entry : counterNames.entrySet()) {
      EventRecord metricsEvent = getContext().createEventRecord("custom-metrics", 1, entry.getKey());
      Map<String, Field> metricsRoot = new HashMap<>();
      metricsRoot.put(entry.getKey(), Field.create(getMetricValue(entry.getKey(), entry.getValue())));
      metricsEvent.set(Field.create(metricsRoot));
      getContext().toEvent(metricsEvent);
    }
  }

  private long getMetricValue(String metricName, MetricType type) {
    switch (type) {
      case COUNTER:
        return getContext().getCounter(metricName).getCount();
      case METER:
        return getContext().getMeter(metricName).getCount();
      default:
        throw new IllegalArgumentException(Utils.format("Unsupported metric: '{}'", type));
    }
  }

  @Override
  protected void process(Record record, SingleLaneBatchMaker batchMaker) throws StageException {
    for (MetricConfig metricConfig : getConf().counters) {
      RecordEL.setRecordInContext(conditionVars, record);
      if (!conditionEval.eval(conditionVars, metricConfig.condition, Boolean.class)) {
        continue;
      }
      RecordEL.setRecordInContext(metricNameVars, record);
      String counterName = metricNameEval.eval(metricNameVars, metricConfig.name, String.class);
      checkState(!Strings.isNullOrEmpty(counterName), "Group By expression evaluated to null or empty string.");

      increment(counterName, metricConfig.type);
    }

    batchMaker.addRecord(record);
  }

  private void increment(String name, MetricType type) {
    switch (type) {
      case COUNTER:
        Counter counter = getContext().getCounter(name);
        if (counter == null) {
          counter = getContext().createCounter(name);
          counterNames.put(name, type);
        }
        counter.inc();
        break;
      case METER:
        Meter meter = getContext().getMeter(name);
        if (meter == null) {
          meter = getContext().createMeter(name);
          counterNames.put(name, type);
        }
        meter.mark();
        break;
      default:
        throw new IllegalArgumentException(Utils.format("Unsupported metric '{}'", type));
    }
  }
}
