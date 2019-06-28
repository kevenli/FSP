from threading import Thread
import time
import logging

logger = logging.getLogger(__name__)

class JobWorkerWatcher:
    def __init__(self):
        self._watch_thread = Thread(target=self._check_worker_state)
        self._jobs = {}
        self.on_job_finished = None

    def start(self):
        self._watch_thread = Thread(target=self._check_worker_state)
        self._watch_thread.start()

    def _check_worker_state(self):
        while True:
            job_instances_to_remove = []
            for job_instance, job_worker in self._jobs.items():
                if job_worker.finished:
                    logger.debug('job worker finished.')
                    self._fire_job_finished(job_instance)
                    job_instances_to_remove.append(job_instance)

            for job_instance in job_instances_to_remove:
                del self._jobs[job_instance]

            time.sleep(3)

    def watchWorker(self, job_instance, job_worker):
        self._jobs[job_instance] = job_worker

    def _fire_job_finished(self, job_instance):
        if self.on_job_finished:
            self.on_job_finished(job_instance)


