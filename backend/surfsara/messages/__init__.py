from dataclasses import dataclass
from dataclasses_json import dataclass_json, LetterCase


@dataclass_json(letter_case=LetterCase.CAMEL)
@dataclass
class StartContainer:
    task_id: str
    data_path: str
    code_path: str
    code_hash: dict


@dataclass_json
@dataclass
class AnalyzeArtifact:
    permission_id: str
