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
 * Windows implementation of SyslogLogListener.
 * Delegates to EventLogLogListener since Windows uses Event Log instead of syslog.
 * The facility parameter is ignored as Windows Event Log doesn't have this concept.
 *
 * @param ident Used as the event source name in Event Viewer
 * @param facility Ignored on Windows
 */
@OptIn(ExperimentalTime::class)
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class SyslogLogListener actual constructor(
    ident: String,
    @Suppress("UNUSED_PARAMETER") facility: SyslogFacility
) : ILogListener by EventLogLogListener(source = ident)