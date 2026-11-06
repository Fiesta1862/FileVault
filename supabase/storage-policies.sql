-- Create a PRIVATE Storage bucket named 'filevault' in the Supabase dashboard first.
-- Each signed-in user can only upload/read/delete objects inside their own UUID folder.
create policy "FileVault users can read own encrypted objects"
on storage.objects for select to authenticated
using (bucket_id = 'filevault' and (storage.foldername(name))[1] = auth.uid()::text);

create policy "FileVault users can upload own encrypted objects"
on storage.objects for insert to authenticated
with check (bucket_id = 'filevault' and (storage.foldername(name))[1] = auth.uid()::text);

create policy "FileVault users can update own encrypted objects"
on storage.objects for update to authenticated
using (bucket_id = 'filevault' and (storage.foldername(name))[1] = auth.uid()::text)
with check (bucket_id = 'filevault' and (storage.foldername(name))[1] = auth.uid()::text);

create policy "FileVault users can delete own encrypted objects"
on storage.objects for delete to authenticated
using (bucket_id = 'filevault' and (storage.foldername(name))[1] = auth.uid()::text);