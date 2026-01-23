/*
 * Copyright (c) 2017-2025 DR-ONLINE SP. Z O.O.
 * Copyright (c) 2017-2025 Przemysław Dobrowolski
 *
 * SPDX-License-Identifier: MIT
 */

package pl.dronline.utils.log.listener

import pl.dronline.utils.log.ILogListener
import kotlin.time.ExperimentalTime

/**
 * Windows implementation of LogcatLogListener.
 * Delegates to EventLogLogListener (Windows Event Log).
 */
@OptIn(ExperimentalTime::class)
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class LogcatLogListener actual constructor() : ILogListener by EventLogLogListener()