package org.ooni.probe.background

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.async
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.di.Dependencies
import platform.BackgroundTasks.BGProcessingTask
import platform.Foundation.NSDate
import platform.Foundation.NSOperationQueue
import platform.Foundation.timeIntervalSinceDate
import platform.UIKit.UIApplication
import io.sentry.kotlin.multiplatform.Sentry

class OperationsManager(private val dependencies: Dependencies, private val backgroundRunner: BackgroundRunner) {
    // https://developer.apple.com/documentation/foundation/nsdate/1410206-timeintervalsince
    fun startSingleRun(spec: RunSpecification) {
        val startDateTime = NSDate()
        val differed = CoroutineScope(Dispatchers.Default).async {
            try {
                dependencies.runBackgroundTask(spec).collect()
                Logger.i { "Job finished successfully in ${NSDate().timeIntervalSinceDate(startDateTime)} ms" }
                Sentry.captureMessage("Job finished")
            } catch (e: Exception) {
                Logger.e(e) { "Job failed with exception in ${NSDate().timeIntervalSinceDate(startDateTime)} ms" }
                Sentry.captureMessage("Job failed")
            }
        }
        backgroundRunner(background = {
            differed.start()
        }, cancel = {
            differed.cancel()
            Logger.i { "Job cancelled after ${NSDate().timeIntervalSinceDate(startDateTime)} ms" }
            Sentry.captureMessage("Job Canceled")
        })
    }

    fun handleAutorunTask(task: BGProcessingTask) {
        Logger.d { "Handling autorun task" }
        val startDateTime = NSDate()
        val differed = CoroutineScope(Dispatchers.Default).async {
            try {
                dependencies.runBackgroundTask(null).collect()
                Logger.i { "Job finished successfully in ${NSDate().timeIntervalSinceDate(startDateTime)} ms" }
                task.setTaskCompletedWithSuccess(true)
                Sentry.captureMessage("Job finished")
            } catch (e: Exception) {
                Logger.e(e) { "Job failed with exception in ${NSDate().timeIntervalSinceDate(startDateTime)} ms" }
                task.setTaskCompletedWithSuccess(false)
                Sentry.captureMessage("Job failed")
            }
        }
        backgroundRunner(background = {
            differed.start()
        }, cancel = {
            differed.cancel()
            Logger.i { "Job cancelled after ${NSDate().timeIntervalSinceDate(startDateTime)} ms" }
            Sentry.captureMessage("Job Canceled")
        })
    }

    fun handleUpdateDescriptorTask(task: BGProcessingTask) {
        val testDescriptorRepository by lazy { dependencies.testDescriptorRepository }
        Logger.d { "Handling update descriptor task" }
        val operationQueue = NSOperationQueue()

        val getDescriptorUpdate by lazy { dependencies.getDescriptorUpdate }
        val operation = DescriptorUpdateOperation(
            testDescriptorRepository = testDescriptorRepository,
            fetchDescriptorUpdate = getDescriptorUpdate,
        )

        task.expirationHandler = { operation.cancel() }
        operation.completionBlock = { task.setTaskCompletedWithSuccess(!operation.isCancelled()) }
        operationQueue.addOperation(operation)
    }

    fun fetchDescriptorUpdate(descriptors: List<InstalledTestDescriptorModel>?) {
        Logger.d("Fetching descriptor update")
        val operationQueue = NSOperationQueue()
        val getDescriptorUpdate by lazy { dependencies.getDescriptorUpdate }
        val testDescriptorRepository by lazy { dependencies.testDescriptorRepository }
        val operation = DescriptorUpdateOperation(
            descriptors = descriptors,
            fetchDescriptorUpdate = getDescriptorUpdate,
            testDescriptorRepository = testDescriptorRepository,
        )
        val identifier = UIApplication.sharedApplication.beginBackgroundTaskWithExpirationHandler {
            operation.cancel()
        }
        operation.completionBlock = {
            UIApplication.sharedApplication.endBackgroundTask(identifier)
        }
        operationQueue.addOperation(operation)
    }
}
