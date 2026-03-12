import re

with open("db_dump.sql", "r") as f:
    text = f.read()

# Replace any occurrence of 'elly.' with 'public.'
text = re.sub(r'\belly\.', 'public.', text)

# Replace any occurrence of 'SCHEMA elly' with 'SCHEMA public'
text = re.sub(r'\bSCHEMA elly\b', 'SCHEMA public', text)

with open("temp_dump.sql", "w") as f:
    f.write(text)
