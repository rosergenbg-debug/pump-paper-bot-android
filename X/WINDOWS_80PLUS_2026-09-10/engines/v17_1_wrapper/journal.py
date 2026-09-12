"""50%-on-each-market archive policy. Preserve V16 search scores and frontier.

The full research result is retained for journal entries and the engine's original
40 working leaders. Other completed trials keep only id/family/score for resume.
No replay, reward, ranking or execution function is replaced.
"""
import heapq
import json
import math
import zlib

REQUIRED = ('BEAR','SIDEWAYS','BULL')
FRONTIER = 40  # Existing V16 run() leader count, not an evaluation budget.


def decode(payload):
    return json.loads(zlib.decompress(payload) if isinstance(payload, bytes) else payload)


def qualifies(result):
    regimes = result.get('regimes',{})
    try:
        return all(math.isfinite(float(regimes[name]['metrics']['compound_net']))
                   and float(regimes[name]['metrics']['compound_net']) >= .5 for name in REQUIRED)
    except (KeyError, TypeError, ValueError):
        return False


def summary(result):
    return dict(id=result['id'], config={'entry_family':result['config'].get('entry_family','DIP')},
                score=result['score'], positive=result.get('positive',False), eligible=result.get('eligible',False),
                worst_net=result.get('worst_net'), origin=result.get('origin',''),
                completed_at=result.get('completed_at'), _retention='SCORE_ONLY_NOT_A_STRATEGY')


def pack(result):
    return zlib.compress(json.dumps(result,separators=(',',':')).encode(),9)


def ranked_key(rowid, cid, result):
    return (tuple(result['score']), -rowid, cid)


def frontier(db):
    heap = []
    for rowid,cid,payload in db.execute('SELECT rowid,id,result FROM trials WHERE result IS NOT NULL'):
        result = decode(payload)
        key = ranked_key(rowid,cid,result)
        if len(heap)<FRONTIER:
            heapq.heappush(heap,key)
        elif key>heap[0]:
            heapq.heapreplace(heap,key)
    return heap


def compact(db, apply=False):
    keep = {item[2] for item in frontier(db)}
    stats = dict(completed=0,journal_50_each=0,working_frontier=0,details_removed=0,already_compact=0)
    changes = []
    for cid,payload in db.execute('SELECT id,result FROM trials WHERE result IS NOT NULL'):
        result = decode(payload); stats['completed']+=1
        accepted = qualifies(result)
        if accepted:
            stats['journal_50_each']+=1
        elif cid in keep:
            if '_retention' in result:
                raise ValueError('Working frontier lost its full result; restore backup')
            stats['working_frontier']+=1
        elif '_retention' in result:
            stats['already_compact']+=1
        else:
            stats['details_removed']+=1
            if apply:
                short=summary(result)
                changes.append((pack(short),json.dumps(short['config']),cid))
        if apply:
            db.execute('UPDATE trials SET positive=? WHERE id=?',(int(accepted),cid))
    if apply:
        db.executemany('UPDATE trials SET result=?,config=? WHERE id=?',changes)
        db.execute('INSERT OR REPLACE INTO meta(key,value) VALUES(?,?)',
                   ('journal_policy','NET>=0.50 on BEAR,SIDEWAYS,BULL; full V16 frontier retained; score-only completed memory'))
    return stats


def install():
    """Explicit storage adapter; all original V16 code files remain immutable."""
    from pump_research_lab import research_v12
    if getattr(research_v12.Ledger,'journal_policy',False):
        return research_v12.Ledger
    OriginalLedger=research_v12.Ledger

    class JournalLedger(OriginalLedger):
        journal_policy=True
        original_class=OriginalLedger

        def __init__(self,path):
            super().__init__(path)
            self._frontier=frontier(self.db)

        def finish(self,result):
            row=self.db.execute('SELECT rowid FROM trials WHERE id=?',(result['id'],)).fetchone()
            if row is None:
                raise ValueError('Unknown result identity')
            key=ranked_key(row[0],result['id'],result)
            evicted=None
            present=any(x[2]==result['id'] for x in self._frontier)
            working=present or len(self._frontier)<FRONTIER or key>self._frontier[0]
            if working and not present:
                if len(self._frontier)<FRONTIER:
                    heapq.heappush(self._frontier,key)
                else:
                    evicted=heapq.heapreplace(self._frontier,key)[2]
            accepted=qualifies(result)
            stored=result if accepted or working else summary(result)
            with self.db:
                self.db.execute('UPDATE trials SET result=?,config=?,positive=? WHERE id=?',
                    (pack(stored),json.dumps(stored['config']),int(accepted),result['id']))
                if evicted:
                    prior=decode(self.db.execute('SELECT result FROM trials WHERE id=?',(evicted,)).fetchone()[0])
                    if not qualifies(prior):
                        short=summary(prior)
                        self.db.execute('UPDATE trials SET result=?,config=?,positive=0 WHERE id=?',
                            (pack(short),json.dumps(short['config']),evicted))
    research_v12.Ledger=JournalLedger
    return JournalLedger
