"""Make the workflow scripts in the parent tools/ importable to pytest."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
