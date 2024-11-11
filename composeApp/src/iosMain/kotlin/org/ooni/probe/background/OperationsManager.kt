package org.ooni.probe.background

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.di.Dependencies
import platform.BackgroundTasks.BGProcessingTask
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication

class OperationsManager(private val dependencies: Dependencies, private val backgroundRunner: BackgroundRunner) {
    fun startSingleRun(spec: RunSpecification) {
        backgroundRunner(background = {
            runBlocking { dependencies.runBackgroundTask(spec).collect() }
        })
    }

    fun handleAutorunTask(task: BGProcessingTask) {
        Logger.d { "Handling autorun task" }
        backgroundRunner(background = {
            runBlocking { dependencies.runBackgroundTask(null).collect() }
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
