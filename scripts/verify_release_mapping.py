# Checks release mapping expectations so shrinking/obfuscation regressions can be caught during release validation.

"""Check release R8 mappings for Firestore's reflection-based property contract."""
from pathlib import Path
import re
import sys

mapping = Path(sys.argv[1]) if len(sys.argv) > 1 else Path('app/build/outputs/mapping/release/mapping.txt')
models = {
    'com.invatech.pockit.data.local.database.FinanceRecord': ['getId', 'getOwner', 'getKind', 'getName', 'getAmountPaise', 'getDirection', 'getCategory', 'getNote', 'getStatus', 'getOccurredAt'],
    'com.invatech.pockit.data.local.entity.CategoryBudgetEntity': ['getCategory', 'getMonthlyLimitPaise', 'getWarningThresholdPercent', 'getUpdatedAtMillis'],
    'com.invatech.pockit.data.local.entity.SubscriptionEntity': ['getId', 'getTitle', 'getAmountPaise', 'getBillingCycle', 'getDueDayOfMonth', 'getCategory', 'getReminderDaysBefore', 'getPaymentMethod', 'getNotes', 'isActive', 'getCreatedAtMillis', 'getNextDueMillis'],
    'com.invatech.pockit.data.local.entity.NotificationEntity': ['getId', 'getTitle', 'getBody', 'getType', 'getTimestamp', 'isRead', 'getTargetId'],
}
blocks = {name: [] for name in models}
current = None
errors = []
with mapping.open(encoding='utf-8') as source:
    for line in source:
        if line and not line[0].isspace() and not line.startswith('#'):
            header = re.match(r'(.+) -> (.+):\s*$', line)
            current = header.group(1) if header and header.group(1) in models else None
            if current and header.group(2) != current:
                errors.append(f'{current}: class name changed to {header.group(2)}')
        if current:
            blocks[current].append(line)
for name, getters in models.items():
    block = ''.join(blocks[name])
    if not block:
        errors.append(f'{name}: missing from mapping')
    for getter in getters:
        if not re.search(r'\b' + re.escape(getter) + r'\(\).* -> ' + re.escape(getter) + r'\s*$', block, re.M):
            errors.append(f'{name}: reflective getter {getter} removed or renamed')
if errors:
    print('\n'.join(errors))
    sys.exit(1)
print(f'PASS: {len(models)} Firestore model classes and {sum(map(len, models.values()))} getters retained.')
