


class FSPException(Exception):
    def __init__(self, message='FSPException'):
        super(FSPException, self).__init__(message)

class FSPFatalException(FSPException):
    def __init__(self, message='FSPFatalException'):
        super(FSPFatalException, self).__init__(message)

class JobNotFoudException(FSPException):
    def __init__(self, message='Job not found.'):
        super(JobNotFoudException, self).__init__(message)

class JobRunFailedException(FSPException):
    def __init__(self, message='Job start failed.'):
        super(JobRunFailedException, self).__init__(message)

class JobStillRunning(FSPException):
    def __init__(self, message='Job still running. '):
        super(JobStillRunning, self).__init__(message)