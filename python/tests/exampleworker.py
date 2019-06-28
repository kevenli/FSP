
from fsp.client import AsyncClient, Task, Client
from fsp.worker import ThreadWorker
import logging
import time
import threading
import sys


logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)
#logger.setLevel(logging.DEBUG)


def job_start():
    logger.info("start")
    time.sleep(10)
    logger.info('Still running')
    time.sleep(10)
    logger.info('Still running')
    time.sleep(10)
    logger.info('Finished')



def main():
    client = AsyncClient('localhost', 'abc', '123')
    def register_job():
        job = Task('Ajob', '*/5 * * * * ?')
        client.register_task(job, job_start)

    #client.connect(callback=register_job)
    client.connect()
    client.start()

def flow1_job(x):
    print(x)
    sys.stdout.flush()

def flow1():
    client = Client('localhost', 'abc', '123')
    client.connect()
    for x in range(3):
        task = Task('job_%s' % x, '*/5 * * * * ?')
        task_worker = ThreadWorker(flow1_job, x)
        client.register_task(task, task_worker)
    client.wait_till_end()


if __name__ == '__main__':
    #main()
    flow1()