from threading import Thread
import logging

from concurrent.futures import ThreadPoolExecutor

logger = logging.getLogger(__name__)

class Worker:
    def start(self):
        pass

    @property
    def finished(self):
        return True

executor = ThreadPoolExecutor(5)

class ThreadWorker(Worker):
    def __init__(self, target, *args, **kwargs):
        self._thread = Thread(target=target, args=args, kwargs=kwargs)
        self._target = target
        self._args = args
        self._kwargs = kwargs
        self._started = False
        self._future = None

    def start(self):
        logger.debug('job worker starting.')
        self._started = True
        #self._thread.run()
        self._future = executor.submit(self._target, self.args, self._kwargs)

    @property
    def finished(self):
        #return self._thread.isAlive()
        # if not self._started:
        #     return True
        # return not self._thread.isAlive()
        return self._future and self._future.done()