
from fsp.client import AsyncClient, Task, Client
import logging
import time
import threading

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)

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

def flow1_job(job):
    print(job.x)

def flow1():
    client = Client('localhost', 'abc', '123')
    client.connect()
    for x in range(10):
        task = Task('job_%s' % x, '*/5 * * * * ?')
        task.x = x
        client.register_task(task, flow1_job)
    client.wait_till_end()


if __name__ == '__main__':
    #main()
    flow1()