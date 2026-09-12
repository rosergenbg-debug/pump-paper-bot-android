import hashlib
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from bootstrap import load_original
load_original()
from offline_data import select_existing, verify_existing, signature, SYMBOLS


class OfflineTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        (self.root/'data'/'prepared').mkdir(parents=True)
        self.bundle = dict(months=6,market_regime='BEAR',prepared_at_utc='2000-01-01T00:00:00Z',
                           period=dict(start_ms=0,end_ms=0),datasets={})
        for symbol in SYMBOLS:
            raw = self.root/(symbol+'.jsonl'); raw.write_bytes(b'{"fixture":true}\n')
            sha = hashlib.sha256(raw.read_bytes()).hexdigest()
            manifest = self.root/(symbol+'.manifest.json')
            manifest.write_text(json.dumps(dict(symbol=symbol,interval='1m',start_utc='1970-01-01T00:00:00Z',
                end_utc='1970-01-01T00:00:59.999Z',row_count=1,checksum_sha256=sha,gaps=[],duplicates=[])))
            self.bundle['datasets'][symbol] = dict(raw_path=str(raw),manifest_path=str(manifest),checksum_sha256=sha,row_count=1)
        self.descriptor = self.root/'data'/'prepared'/'research-base-6m-bear.json'
        self.descriptor.write_text(json.dumps(self.bundle))

    def tearDown(self):
        self.temp.cleanup()

    def test_old_cache_is_accepted_without_network_or_mutation(self):
        original = self.descriptor.read_bytes()
        with patch('urllib.request.urlopen', side_effect=AssertionError('network')):
            self.assertEqual(list(verify_existing(self.root,6,['BEAR'])),['BEAR'])
        self.assertEqual(self.descriptor.read_bytes(),original)

    def test_missing_regime_does_not_download(self):
        with self.assertRaisesRegex(ValueError,'Нет готовых'):
            verify_existing(self.root,6,['BEAR','BULL'])

    def test_corruption_is_rejected(self):
        Path(self.bundle['datasets']['BTCUSDT']['raw_path']).write_bytes(b'corrupted')
        with self.assertRaisesRegex(ValueError,'сумма'):
            verify_existing(self.root,6,['BEAR'])

    def test_other_date_context_is_rejected(self):
        file = Path(self.bundle['datasets']['SOLUSDT']['manifest_path'])
        value = json.loads(file.read_text()); value['start_utc']='1970-01-02T00:00:00Z'
        file.write_text(json.dumps(value))
        with self.assertRaisesRegex(ValueError,'даты контекста'):
            verify_existing(self.root,6,['BEAR'])

    def test_gap_context_is_rejected(self):
        file = Path(self.bundle['datasets']['SOLUSDT']['manifest_path'])
        value = json.loads(file.read_text()); value['gaps']=[123]
        file.write_text(json.dumps(value))
        with self.assertRaisesRegex(ValueError,'пропуски'):
            verify_existing(self.root,6,['BEAR'])

    def test_stop_does_not_delete_files(self):
        from pump_research_lab.optimizer import ResearchCancelled
        with self.assertRaises(ResearchCancelled):
            verify_existing(self.root,6,['BEAR'],cancel=lambda:True)
        self.assertTrue(self.descriptor.exists())

    def test_stat_identity_changes_with_file(self):
        selected = select_existing(self.root,6,['BEAR']); before = signature(selected)
        Path(self.bundle['datasets']['PUMPUSDT']['raw_path']).write_bytes(b'changed')
        self.assertNotEqual(signature(selected),before)

    def test_engine_methods_unchanged(self):
        from restored_app import RestoredWindow, original
        for name in ('start','retest','toggle_pause','stop','check_robustness','export_selected','show_parameters'):
            self.assertIs(getattr(RestoredWindow,name),getattr(original.ResearchWindow,name))


if __name__=='__main__':
    unittest.main()
