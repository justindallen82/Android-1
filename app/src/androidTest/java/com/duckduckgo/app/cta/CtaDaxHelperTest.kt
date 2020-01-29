/*
 * Copyright (c) 2020 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.app.cta

import android.content.res.Resources
import androidx.fragment.app.FragmentActivity
import com.duckduckgo.app.cta.ui.DaxBubbleCta
import com.duckduckgo.app.cta.ui.HomePanelCta
import com.duckduckgo.app.global.install.AppInstallStore
import com.duckduckgo.app.onboarding.store.OnboardingStore
import com.duckduckgo.app.privacy.model.TestEntity
import com.duckduckgo.app.trackerdetection.model.TrackingEvent
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

class CtaDaxHelperTest {
    @Mock
    private lateinit var mockOnboardingStore: OnboardingStore

    @Mock
    private lateinit var mockAppInstallStore: AppInstallStore

    @Mock
    private lateinit var mockActivity: FragmentActivity

    @Mock
    private lateinit var mockResources: Resources

    private lateinit var testee: CtaDaxHelper

    @Before
    fun before() {
        MockitoAnnotations.initMocks(this)

        testee = CtaDaxHelper(mockOnboardingStore, mockAppInstallStore)

        whenever(mockActivity.resources).thenReturn(mockResources)
        whenever(mockResources.getString(any())).thenReturn("withZero")
        whenever(mockResources.getQuantityString(any(), any(), any())).thenReturn("withMultiple")
    }

    @Test
    fun whenAddCtaToHistoryThenReturnCorrectValue() {
        whenever(mockOnboardingStore.onboardingDialogJourney).thenReturn(null)
        whenever(mockAppInstallStore.installTimestamp).thenReturn(System.currentTimeMillis())

        val value = testee.addCtaToHistory("test")
        assertEquals("test:0", value)
    }

    @Test
    fun whenAddCtaToHistoryOnDay3ThenReturnCorrectValue() {
        whenever(mockOnboardingStore.onboardingDialogJourney).thenReturn(null)
        whenever(mockAppInstallStore.installTimestamp).thenReturn(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3))

        val value = testee.addCtaToHistory("test")
        assertEquals("test:3", value)
    }

    @Test
    fun whenAddCtaToHistoryOnDay4ThenReturn3AsDayValue() {
        whenever(mockOnboardingStore.onboardingDialogJourney).thenReturn(null)
        whenever(mockAppInstallStore.installTimestamp).thenReturn(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(4))

        val value = testee.addCtaToHistory("test")
        assertEquals("test:3", value)
    }

    @Test
    fun whenAddCtaToHistoryContainsHistoryThenConcatenateNewValue() {
        val ctaHistory = "s:0-t:1"
        whenever(mockOnboardingStore.onboardingDialogJourney).thenReturn(ctaHistory)
        whenever(mockAppInstallStore.installTimestamp).thenReturn(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1))

        val value = testee.addCtaToHistory("test")
        val expectedValue = "$ctaHistory-test:1"

        assertEquals(expectedValue, value)
    }

    @Test
    fun whenCanSendPixelAndCtaNotPartOfHistoryThenReturnTrue() {
        whenever(mockOnboardingStore.onboardingDialogJourney).thenReturn("s:0")
        assertTrue(testee.canSendPixel(DaxBubbleCta.DaxEndCta(testee)))
    }

    @Test
    fun whenCanSendPixelAndCtaIsPartOfHistoryThenReturnFalse() {
        whenever(mockOnboardingStore.onboardingDialogJourney).thenReturn("e:0")
        assertFalse(testee.canSendPixel(DaxBubbleCta.DaxEndCta(testee)))
    }

    @Test
    fun whenCanSendPixelAndCtaIsHomePanelTypeThenReturnTrue() {
        assertTrue(testee.canSendPixel(HomePanelCta.AddWidgetAuto))
    }

    @Test
    fun whenGetNetworkPercentageForGoogleThenReturn90() {
        assertEquals("90%", testee.getNetworkPercentage("Google"))
    }

    @Test
    fun whenGetNetworkPercentageForFacebookThenReturn40() {
        assertEquals("40%", testee.getNetworkPercentage("Facebook"))
    }

    @Test
    fun whenIsFromSameNetworkDomainForFacebookThenReturnTrue() {
        assertTrue(testee.isFromSameNetworkDomain("facebook"))
    }

    @Test
    fun whenIsFromSameNetworkDomainForGoogleThenReturnTrue() {
        assertTrue(testee.isFromSameNetworkDomain("google"))
    }

    @Test
    fun whenIsFromSameNetworkDomainForAmazonThenReturnFalse() {
        assertFalse(testee.isFromSameNetworkDomain("amazon"))
    }

    @Test
    fun whenMoreThanTwoMajorTrackersBlockedReturnFirstTwoWithMultipleString() {
        val trackers = listOf(
            TrackingEvent("facebook.com", "facebook.com", blocked = true, entity = TestEntity("Facebook", "Facebook", 9.0), categories = null),
            TrackingEvent("other.com", "other.com", blocked = true, entity = TestEntity("Other", "Other", 9.0), categories = null),
            TrackingEvent("amazon.com", "amazon.com", blocked = true, entity = TestEntity("Amazon", "Amazon", 9.0), categories = null)
        )

        val value = testee.getTrackersBlockedCtaText(mockActivity, trackers)
        assertEquals("<b>Facebook, Other</b>withMultiple", value)
    }

    @Test
    fun whenTwoMajorTrackersBlockedReturnThemWithMultipleString() {
        val trackers = listOf(
            TrackingEvent("facebook.com", "facebook.com", blocked = true, entity = TestEntity("Facebook", "Facebook", 9.0), categories = null),
            TrackingEvent("other.com", "other.com", blocked = true, entity = TestEntity("Other", "Other", 9.0), categories = null)
        )

        val value = testee.getTrackersBlockedCtaText(mockActivity, trackers)
        assertEquals("<b>Facebook, Other</b>withZero", value)
    }

    @Test
    fun whenOneMajorTrackersBlockedReturnItWithMultipleString() {
        val trackers = listOf(
            TrackingEvent("facebook.com", "facebook.com", blocked = true, entity = TestEntity("Facebook", "Facebook", 9.0), categories = null),
            TrackingEvent("other.com", "other.com", blocked = true, entity = TestEntity("Other", "Other", 3.0), categories = null)
        )

        val value = testee.getTrackersBlockedCtaText(mockActivity, trackers)
        assertEquals("<b>Facebook</b>withMultiple", value)
    }

    @Test
    fun whenMultipleTrackersFromSameNetworkBlockedReturnOnlyOneWithMultipleString() {
        val trackers = listOf(
            TrackingEvent("facebook.com", "facebook.com", blocked = true, entity = TestEntity("Facebook", "Facebook", 9.0), categories = null),
            TrackingEvent("facebook.com", "facebook.com", blocked = true, entity = TestEntity("Facebook", "Facebook", 9.0), categories = null),
            TrackingEvent("facebook.com", "facebook.com", blocked = true, entity = TestEntity("Facebook", "Facebook", 9.0), categories = null)
        )

        val value = testee.getTrackersBlockedCtaText(mockActivity, trackers)
        assertEquals("<b>Facebook</b>withMultiple", value)
    }
}