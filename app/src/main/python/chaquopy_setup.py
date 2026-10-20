"""Chaquopy bridge helpers.

These functions exist because Chaquopy cannot invoke Python builtins like ``exec``
through ``PyObject.callAttr`` (it raises ``SystemError: frame does not exist``).
Calling them as ordinary module functions via ``Python.getModule()`` works reliably.
"""


def setup_path(output_dir=None):
    """Appends the engine's working directory to sys.path."""
    import sys

    if output_dir and output_dir not in sys.path:
        sys.path.append(output_dir)