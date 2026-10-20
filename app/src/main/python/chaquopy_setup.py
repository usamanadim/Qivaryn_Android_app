"""Chaquopy bridge helpers.

These functions exist because Chaquopy cannot invoke Python builtins like ``exec``
through ``PyObject.callAttr`` (it raises ``SystemError: frame does not exist``).
Calling them as ordinary module functions via ``Python.getModule()`` works reliably.
"""


def setup_path(ffmpeg_dir=None, output_dir=None):
    """Inject the ffmpeg directory into PATH and append output_dir to sys.path."""
    import os
    import sys

    if ffmpeg_dir:
        os.environ["PATH"] = ffmpeg_dir + os.pathsep + os.environ.get("PATH", "")
    if output_dir and output_dir not in sys.path:
        sys.path.append(output_dir)