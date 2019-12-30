from dataclasses import dataclass
from dataclasses_json import dataclass_json, LetterCase


@dataclass
@dataclass_json(letter_case=LetterCase.CAMEL)
class StartContainer:
    task_id: str
    data_path: str
    code_path: str
    code_hash: dict


@dataclass
@dataclass_json
class AnalyzeArtifact:
    permission_id: str
