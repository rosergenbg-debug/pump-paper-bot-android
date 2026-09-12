from dataclasses import asdict
from pathlib import Path
import tempfile
import unittest
from bootstrap import load_original
load_original()
from journal import install, qualifies, compact, decode
from pump_research_lab.baseline_x import EconomyConfig
from pump_research_lab.research_v12 import learning_reward

Ledger=install()


class JournalTests(unittest.TestCase):
    def test_requires_each_market_and_finite_net(self):
        def r(values):
            return {'regimes':{n:{'metrics':{'compound_net':v}} for n,v in zip(('BEAR','SIDEWAYS','BULL'),values)}}
        self.assertTrue(qualifies(r([.5,.7,.5])))
        for values in ([.49999,2,2],[1,1],[1,1,float('nan')],[1,1,float('inf')]):
            self.assertFalse(qualifies(r(values)))

    def populate(self,ledger,count=70):
        originals=[]
        for i in range(count):
            config=asdict(EconomyConfig(target_net=.02+i*.001)); cid=ledger.add(config,'TEST')
            result=dict(id=cid,config=config,score=[1,i/100,i/100],positive=True,eligible=True,worst_net=i/100,
                regimes={n:{'metrics':{'compound_net':i/100},'folds':{'evidence':'x'*100}} for n in ('BEAR','SIDEWAYS','BULL')},
                origin='TEST',completed_at=i)
            ledger.finish(result); originals.append(result)
        return originals

    def check_compatible(self,ledger,originals):
        results=list(ledger.results())
        self.assertEqual(len(results),len(originals))
        self.assertEqual([learning_reward(r) for r in results],[learning_reward(r) for r in originals])
        top=lambda values:sorted(values,key=lambda r:r['score'],reverse=True)[:40]
        self.assertEqual(top(results),top(originals))
        self.assertFalse(list(ledger.pending()))
        self.assertIsNone(ledger.add(originals[0]['config'],'REPEAT'))
        self.assertTrue(all(qualifies(r) for r in ledger.results(True)))
        self.assertEqual(len(list(ledger.results(True))),20)

    def test_new_storage_preserves_learning_and_dedup_after_restart(self):
        with tempfile.TemporaryDirectory() as temp:
            path=Path(temp)/'research.sqlite3'; ledger=Ledger(path)
            originals=self.populate(ledger)
            self.check_compatible(ledger,originals)
            ledger.db.close()
            ledger=Ledger(path)
            self.check_compatible(ledger,originals)
            ledger.db.close()

    def test_cleanup_preserves_scores_frontier_pending_and_is_idempotent(self):
        with tempfile.TemporaryDirectory() as temp:
            ledger=Ledger.original_class(Path(temp)/'old.sqlite3')
            originals=self.populate(ledger)
            before=compact(ledger.db)
            self.assertEqual(before['details_removed'],30)
            with ledger.db:
                actual=compact(ledger.db,True)
            self.assertEqual(before,actual)
            self.check_compatible(ledger,originals)
            self.assertEqual(compact(ledger.db)['details_removed'],0)
            ledger.db.close()


if __name__=='__main__':
    unittest.main()
