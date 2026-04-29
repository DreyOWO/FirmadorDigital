# 🗄️ Database Complete Schema - Supabase

Este documento contiene TODO el schema de la base de datos listo para ejecutar en Supabase.

## 📁 Estructura

```
supabase/
├── migrations/
│   ├── 001_initial_schema.sql
│   ├── 002_rls_policies.sql
│   ├── 003_functions.sql
│   └── 004_seed_data.sql
├── storage/
│   └── buckets.sql
└── README.md
```

---

## 🚀 MIGRATION 001: Initial Schema

### supabase/migrations/001_initial_schema.sql

```sql
-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================
-- USERS TABLE
-- ============================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'signer' CHECK (role IN ('admin', 'signer', 'viewer')),
    certificate_id VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index for faster email lookups
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_is_active ON users(is_active);

-- ============================================
-- SIGNATURE WORKFLOWS TABLE
-- ============================================
CREATE TABLE signature_workflows (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_workflows_created_by ON signature_workflows(created_by);
CREATE INDEX idx_workflows_is_active ON signature_workflows(is_active);

-- ============================================
-- WORKFLOW STEPS TABLE
-- ============================================
CREATE TABLE workflow_steps (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workflow_id UUID NOT NULL REFERENCES signature_workflows(id) ON DELETE CASCADE,
    step_order INTEGER NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action_type VARCHAR(50) NOT NULL DEFAULT 'sign' CHECK (action_type IN ('sign', 'approve', 'review')),
    is_required BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(workflow_id, step_order)
);

CREATE INDEX idx_workflow_steps_workflow ON workflow_steps(workflow_id);
CREATE INDEX idx_workflow_steps_user ON workflow_steps(user_id);
CREATE INDEX idx_workflow_steps_order ON workflow_steps(workflow_id, step_order);

-- ============================================
-- DOCUMENTS TABLE
-- ============================================
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    mime_type VARCHAR(100),
    workflow_id UUID REFERENCES signature_workflows(id) ON DELETE SET NULL,
    current_step INTEGER DEFAULT 1,
    status VARCHAR(50) DEFAULT 'pending' CHECK (status IN ('pending', 'in_progress', 'completed', 'rejected')),
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_created_by ON documents(created_by);
CREATE INDEX idx_documents_workflow ON documents(workflow_id);
CREATE INDEX idx_documents_created_at ON documents(created_at DESC);

-- ============================================
-- SIGNATURE HISTORY TABLE
-- ============================================
CREATE TABLE signature_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    step_order INTEGER NOT NULL,
    action VARCHAR(50) NOT NULL CHECK (action IN ('signed', 'rejected', 'approved')),
    comments TEXT,
    signature_data TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_signature_history_document ON signature_history(document_id);
CREATE INDEX idx_signature_history_user ON signature_history(user_id);
CREATE INDEX idx_signature_history_created_at ON signature_history(created_at DESC);

-- ============================================
-- PENDING DOCUMENTS TABLE
-- ============================================
CREATE TABLE pending_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    step_order INTEGER NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    notified_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(document_id, user_id, step_order)
);

CREATE INDEX idx_pending_documents_user ON pending_documents(user_id);
CREATE INDEX idx_pending_documents_document ON pending_documents(document_id);
CREATE INDEX idx_pending_documents_assigned_at ON pending_documents(assigned_at DESC);

-- ============================================
-- NOTIFICATIONS TABLE
-- ============================================
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    document_id UUID REFERENCES documents(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL CHECK (type IN ('pending_signature', 'document_completed', 'document_rejected', 'document_assigned')),
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);

-- ============================================
-- AUDIT LOG TABLE
-- ============================================
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_audit_log_user ON audit_log(user_id);
CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at DESC);

-- ============================================
-- UPDATED_AT TRIGGER FUNCTION
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to tables with updated_at
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_workflows_updated_at
    BEFORE UPDATE ON signature_workflows
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_documents_updated_at
    BEFORE UPDATE ON documents
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

---

## 🔒 MIGRATION 002: Row Level Security Policies

### supabase/migrations/002_rls_policies.sql

```sql
-- ============================================
-- ENABLE ROW LEVEL SECURITY
-- ============================================
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE signature_workflows ENABLE ROW LEVEL SECURITY;
ALTER TABLE workflow_steps ENABLE ROW LEVEL SECURITY;
ALTER TABLE documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE signature_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE pending_documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_log ENABLE ROW LEVEL SECURITY;

-- ============================================
-- USERS POLICIES
-- ============================================

-- Users can view their own profile
CREATE POLICY "Users can view own profile"
ON users FOR SELECT
USING (auth.uid() = id);

-- Users can update their own profile
CREATE POLICY "Users can update own profile"
ON users FOR UPDATE
USING (auth.uid() = id);

-- Admins can view all users
CREATE POLICY "Admins can view all users"
ON users FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM users
        WHERE id = auth.uid() AND role = 'admin'
    )
);

-- Admins can insert users
CREATE POLICY "Admins can insert users"
ON users FOR INSERT
WITH CHECK (
    EXISTS (
        SELECT 1 FROM users
        WHERE id = auth.uid() AND role = 'admin'
    )
);

-- Admins can update users
CREATE POLICY "Admins can update all users"
ON users FOR UPDATE
USING (
    EXISTS (
        SELECT 1 FROM users
        WHERE id = auth.uid() AND role = 'admin'
    )
);

-- ============================================
-- WORKFLOWS POLICIES
-- ============================================

-- Users can view active workflows
CREATE POLICY "Users can view active workflows"
ON signature_workflows FOR SELECT
USING (is_active = true);

-- Admins can manage workflows
CREATE POLICY "Admins can manage workflows"
ON signature_workflows FOR ALL
USING (
    EXISTS (
        SELECT 1 FROM users
        WHERE id = auth.uid() AND role = 'admin'
    )
);

-- ============================================
-- WORKFLOW STEPS POLICIES
-- ============================================

-- Users can view workflow steps
CREATE POLICY "Users can view workflow steps"
ON workflow_steps FOR SELECT
USING (true);

-- Admins can manage workflow steps
CREATE POLICY "Admins can manage workflow steps"
ON workflow_steps FOR ALL
USING (
    EXISTS (
        SELECT 1 FROM users
        WHERE id = auth.uid() AND role = 'admin'
    )
);

-- ============================================
-- DOCUMENTS POLICIES
-- ============================================

-- Users can view documents they created
CREATE POLICY "Users can view own documents"
ON documents FOR SELECT
USING (created_by = auth.uid());

-- Users can view documents assigned to them
CREATE POLICY "Users can view assigned documents"
ON documents FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM pending_documents
        WHERE document_id = documents.id
        AND user_id = auth.uid()
    )
);

-- Users can view documents they signed
CREATE POLICY "Users can view signed documents"
ON documents FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM signature_history
        WHERE document_id = documents.id
        AND user_id = auth.uid()
    )
);

-- Admins can manage all documents
CREATE POLICY "Admins can manage documents"
ON documents FOR ALL
USING (
    EXISTS (
        SELECT 1 FROM users
        WHERE id = auth.uid() AND role = 'admin'
    )
);

-- Users can create documents
CREATE POLICY "Users can create documents"
ON documents FOR INSERT
WITH CHECK (created_by = auth.uid());

-- ============================================
-- SIGNATURE HISTORY POLICIES
-- ============================================

-- Users can view their signature history
CREATE POLICY "Users can view own signature history"
ON signature_history FOR SELECT
USING (user_id = auth.uid());

-- Users can view signature history of documents they created
CREATE POLICY "Users can view signature history of own documents"
ON signature_history FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM documents
        WHERE id = signature_history.document_id
        AND created_by = auth.uid()
    )
);

-- Users can insert their own signature history
CREATE POLICY "Users can insert own signature history"
ON signature_history FOR INSERT
WITH CHECK (user_id = auth.uid());

-- Admins can view all signature history
CREATE POLICY "Admins can view all signature history"
ON signature_history FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM users
        WHERE id = auth.uid() AND role = 'admin'
    )
);

-- ============================================
-- PENDING DOCUMENTS POLICIES
-- ============================================

-- Users can view their pending documents
CREATE POLICY "Users can view own pending documents"
ON pending_documents FOR SELECT
USING (user_id = auth.uid());

-- Admins can manage pending documents
CREATE POLICY "Admins can manage pending documents"
ON pending_documents FOR ALL
USING (
    EXISTS (
        SELECT 1 FROM users
        WHERE id = auth.uid() AND role = 'admin'
    )
);

-- System can insert pending documents
CREATE POLICY "System can insert pending documents"
ON pending_documents FOR INSERT
WITH CHECK (true);

-- System can delete pending documents
CREATE POLICY "System can delete pending documents"
ON pending_documents FOR DELETE
USING (true);

-- ============================================
-- NOTIFICATIONS POLICIES
-- ============================================

-- Users can view their notifications
CREATE POLICY "Users can view own notifications"
ON notifications FOR SELECT
USING (user_id = auth.uid());

-- Users can update their notifications (mark as read)
CREATE POLICY "Users can update own notifications"
ON notifications FOR UPDATE
USING (user_id = auth.uid());

-- System can insert notifications
CREATE POLICY "System can insert notifications"
ON notifications FOR INSERT
WITH CHECK (true);

-- ============================================
-- AUDIT LOG POLICIES
-- ============================================

-- Admins can view audit log
CREATE POLICY "Admins can view audit log"
ON audit_log FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM users
        WHERE id = auth.uid() AND role = 'admin'
    )
);

-- System can insert audit log
CREATE POLICY "System can insert audit log"
ON audit_log FOR INSERT
WITH CHECK (true);
```

---

## ⚙️ MIGRATION 003: Functions and Triggers

### supabase/migrations/003_functions.sql

```sql
-- ============================================
-- NOTIFICATION FUNCTIONS
-- ============================================

-- Function to create notification
CREATE OR REPLACE FUNCTION create_notification(
    p_user_id UUID,
    p_document_id UUID,
    p_type VARCHAR(50),
    p_message TEXT
)
RETURNS UUID AS $$
DECLARE
    notification_id UUID;
BEGIN
    INSERT INTO notifications (user_id, document_id, type, message)
    VALUES (p_user_id, p_document_id, p_type, p_message)
    RETURNING id INTO notification_id;
    
    RETURN notification_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to mark notification as read
CREATE OR REPLACE FUNCTION mark_notification_read(notification_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    UPDATE notifications
    SET is_read = true
    WHERE id = notification_id
    AND user_id = auth.uid();
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to get unread notification count
CREATE OR REPLACE FUNCTION get_unread_notification_count(p_user_id UUID)
RETURNS INTEGER AS $$
BEGIN
    RETURN (
        SELECT COUNT(*)::INTEGER
        FROM notifications
        WHERE user_id = p_user_id
        AND is_read = false
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================
-- DOCUMENT WORKFLOW FUNCTIONS
-- ============================================

-- Function to assign document to next signer
CREATE OR REPLACE FUNCTION assign_document_to_next_signer(
    p_document_id UUID
)
RETURNS BOOLEAN AS $$
DECLARE
    doc_record RECORD;
    next_step_record RECORD;
BEGIN
    -- Get document info
    SELECT * INTO doc_record
    FROM documents
    WHERE id = p_document_id;
    
    IF NOT FOUND THEN
        RETURN false;
    END IF;
    
    -- Get next workflow step
    SELECT * INTO next_step_record
    FROM workflow_steps
    WHERE workflow_id = doc_record.workflow_id
    AND step_order = doc_record.current_step;
    
    IF NOT FOUND THEN
        -- No more steps, mark as completed
        UPDATE documents
        SET status = 'completed',
            completed_at = NOW()
        WHERE id = p_document_id;
        
        -- Notify document creator
        PERFORM create_notification(
            doc_record.created_by,
            p_document_id,
            'document_completed',
            'El documento "' || doc_record.title || '" ha sido completado.'
        );
        
        RETURN true;
    END IF;
    
    -- Assign to next signer
    INSERT INTO pending_documents (document_id, user_id, step_order)
    VALUES (p_document_id, next_step_record.user_id, next_step_record.step_order)
    ON CONFLICT (document_id, user_id, step_order) DO NOTHING;
    
    -- Create notification
    PERFORM create_notification(
        next_step_record.user_id,
        p_document_id,
        'pending_signature',
        'Tienes un nuevo documento pendiente de firma: "' || doc_record.title || '"'
    );
    
    RETURN true;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to move document to next step
CREATE OR REPLACE FUNCTION move_document_to_next_step(
    p_document_id UUID
)
RETURNS BOOLEAN AS $$
DECLARE
    doc_record RECORD;
BEGIN
    -- Get document info
    SELECT * INTO doc_record
    FROM documents
    WHERE id = p_document_id;
    
    IF NOT FOUND THEN
        RETURN false;
    END IF;
    
    -- Remove from current user's pending
    DELETE FROM pending_documents
    WHERE document_id = p_document_id;
    
    -- Move to next step
    UPDATE documents
    SET current_step = current_step + 1,
        status = 'in_progress'
    WHERE id = p_document_id;
    
    -- Assign to next signer
    RETURN assign_document_to_next_signer(p_document_id);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================
-- AUDIT LOG FUNCTIONS
-- ============================================

-- Function to log audit event
CREATE OR REPLACE FUNCTION log_audit_event(
    p_user_id UUID,
    p_action VARCHAR(100),
    p_entity_type VARCHAR(50),
    p_entity_id UUID,
    p_old_values JSONB DEFAULT NULL,
    p_new_values JSONB DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    audit_id UUID;
BEGIN
    INSERT INTO audit_log (
        user_id,
        action,
        entity_type,
        entity_id,
        old_values,
        new_values,
        ip_address,
        user_agent
    )
    VALUES (
        p_user_id,
        p_action,
        p_entity_type,
        p_entity_id,
        p_old_values,
        p_new_values,
        current_setting('request.headers', true)::json->>'x-forwarded-for',
        current_setting('request.headers', true)::json->>'user-agent'
    )
    RETURNING id INTO audit_id;
    
    RETURN audit_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================
-- DOCUMENT TRIGGERS
-- ============================================

-- Trigger to log document changes
CREATE OR REPLACE FUNCTION audit_document_changes()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        PERFORM log_audit_event(
            NEW.created_by,
            'CREATE',
            'document',
            NEW.id,
            NULL,
            to_jsonb(NEW)
        );
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        PERFORM log_audit_event(
            auth.uid(),
            'UPDATE',
            'document',
            NEW.id,
            to_jsonb(OLD),
            to_jsonb(NEW)
        );
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        PERFORM log_audit_event(
            auth.uid(),
            'DELETE',
            'document',
            OLD.id,
            to_jsonb(OLD),
            NULL
        );
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER audit_documents_trigger
    AFTER INSERT OR UPDATE OR DELETE ON documents
    FOR EACH ROW
    EXECUTE FUNCTION audit_document_changes();

-- ============================================
-- SIGNATURE HISTORY TRIGGERS
-- ============================================

-- Trigger to move document to next step after signature
CREATE OR REPLACE FUNCTION handle_signature_completion()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.action = 'signed' THEN
        PERFORM move_document_to_next_step(NEW.document_id);
    ELSIF NEW.action = 'rejected' THEN
        -- Mark document as rejected
        UPDATE documents
        SET status = 'rejected'
        WHERE id = NEW.document_id;
        
        -- Remove from pending
        DELETE FROM pending_documents
        WHERE document_id = NEW.document_id;
        
        -- Notify document creator
        PERFORM create_notification(
            (SELECT created_by FROM documents WHERE id = NEW.document_id),
            NEW.document_id,
            'document_rejected',
            'El documento ha sido rechazado. Motivo: ' || COALESCE(NEW.comments, 'Sin motivo especificado')
        );
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER signature_completion_trigger
    AFTER INSERT ON signature_history
    FOR EACH ROW
    EXECUTE FUNCTION handle_signature_completion();
```

---

## 🗂️ MIGRATION 004: Seed Data

### supabase/migrations/004_seed_data.sql

```sql
-- ============================================
-- SEED DATA
-- ============================================

-- Insert default admin user
INSERT INTO users (
    id,
    email,
    password_hash,
    full_name,
    role,
    is_active
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin@firmador.cr',
    crypt('admin123', gen_salt('bf')),
    'Administrador del Sistema',
    'admin',
    true
) ON CONFLICT (email) DO NOTHING;

-- Insert demo users
INSERT INTO users (
    id,
    email,
    password_hash,
    full_name,
    role,
    is_active
) VALUES 
(
    '00000000-0000-0000-0000-000000000002',
    'juan.perez@ejemplo.cr',
    crypt('demo123', gen_salt('bf')),
    'Juan Pérez Rodríguez',
    'signer',
    true
),
(
    '00000000-0000-0000-0000-000000000003',
    'maria.gonzalez@ejemplo.cr',
    crypt('demo123', gen_salt('bf')),
    'María González López',
    'signer',
    true
),
(
    '00000000-0000-0000-0000-000000000004',
    'carlos.martinez@ejemplo.cr',
    crypt('demo123', gen_salt('bf')),
    'Carlos Martínez Jiménez',
    'signer',
    true
) ON CONFLICT (email) DO NOTHING;

-- Insert demo workflow
INSERT INTO signature_workflows (
    id,
    name,
    description,
    created_by,
    is_active
) VALUES (
    '10000000-0000-0000-0000-000000000001',
    'Flujo de Aprobación Estándar',
    'Flujo de trabajo para documentos que requieren aprobación de múltiples firmantes',
    '00000000-0000-0000-0000-000000000001',
    true
) ON CONFLICT (id) DO NOTHING;

-- Insert workflow steps
INSERT INTO workflow_steps (
    workflow_id,
    step_order,
    user_id,
    action_type,
    is_required
) VALUES 
(
    '10000000-0000-0000-0000-000000000001',
    1,
    '00000000-0000-0000-0000-000000000002',
    'sign',
    true
),
(
    '10000000-0000-0000-0000-000000000001',
    2,
    '00000000-0000-0000-0000-000000000003',
    'sign',
    true
),
(
    '10000000-0000-0000-0000-000000000001',
    3,
    '00000000-0000-0000-0000-000000000004',
    'approve',
    true
) ON CONFLICT (workflow_id, step_order) DO NOTHING;

-- Insert another demo workflow
INSERT INTO signature_workflows (
    id,
    name,
    description,
    created_by,
    is_active
) VALUES (
    '10000000-0000-0000-0000-000000000002',
    'Flujo Simple',
    'Flujo de trabajo simple con un solo firmante',
    '00000000-0000-0000-0000-000000000001',
    true
) ON CONFLICT (id) DO NOTHING;

INSERT INTO workflow_steps (
    workflow_id,
    step_order,
    user_id,
    action_type,
    is_required
) VALUES (
    '10000000-0000-0000-0000-000000000002',
    1,
    '00000000-0000-0000-0000-000000000002',
    'sign',
    true
) ON CONFLICT (workflow_id, step_order) DO NOTHING;
```

---

## 🪣 STORAGE CONFIGURATION

### supabase/storage/buckets.sql

```sql
-- ============================================
-- STORAGE BUCKETS
-- ============================================

-- Create documents bucket
INSERT INTO storage.buckets (id, name, public)
VALUES ('documents', 'documents', false)
ON CONFLICT (id) DO NOTHING;

-- Create signed-documents bucket
INSERT INTO storage.buckets (id, name, public)
VALUES ('signed-documents', 'signed-documents', false)
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- STORAGE POLICIES
-- ============================================

-- Policy for documents bucket
CREATE POLICY "Users can upload documents"
ON storage.objects FOR INSERT
WITH CHECK (
    bucket_id = 'documents' AND
    auth.role() = 'authenticated'
);

CREATE POLICY "Users can view documents they have access to"
ON storage.objects FOR SELECT
USING (
    bucket_id = 'documents' AND
    auth.role() = 'authenticated' AND
    (
        -- Document creator can view
        EXISTS (
            SELECT 1 FROM documents
            WHERE file_path = storage.objects.name
            AND created_by = auth.uid()
        ) OR
        -- Assigned users can view
        EXISTS (
            SELECT 1 FROM documents d
            JOIN pending_documents pd ON d.id = pd.document_id
            WHERE d.file_path = storage.objects.name
            AND pd.user_id = auth.uid()
        ) OR
        -- Users who signed can view
        EXISTS (
            SELECT 1 FROM documents d
            JOIN signature_history sh ON d.id = sh.document_id
            WHERE d.file_path = storage.objects.name
            AND sh.user_id = auth.uid()
        ) OR
        -- Admins can view all
        EXISTS (
            SELECT 1 FROM users
            WHERE id = auth.uid() AND role = 'admin'
        )
    )
);

-- Policy for signed-documents bucket
CREATE POLICY "System can manage signed documents"
ON storage.objects FOR ALL
USING (
    bucket_id = 'signed-documents' AND
    auth.role() = 'authenticated'
);
```

---

## 📋 SETUP INSTRUCTIONS

### 1. Create Supabase Project

1. Go to [supabase.com](https://supabase.com)
2. Create new project
3. Wait for setup to complete
4. Note your project URL and anon key

### 2. Run Migrations

Execute the SQL files in order in the Supabase SQL Editor:

```sql
-- 1. Run 001_initial_schema.sql
-- 2. Run 002_rls_policies.sql  
-- 3. Run 003_functions.sql
-- 4. Run 004_seed_data.sql
-- 5. Run storage/buckets.sql
```

### 3. Configure Authentication

1. Go to Authentication > Settings
2. Enable email authentication
3. Configure email templates (optional)
4. Set up custom SMTP (optional)

### 4. Test the Setup

```sql
-- Test user login
SELECT * FROM users WHERE email = 'admin@firmador.cr';

-- Test workflows
SELECT w.*, ws.step_order, u.full_name
FROM signature_workflows w
JOIN workflow_steps ws ON w.id = ws.workflow_id
JOIN users u ON ws.user_id = u.id
ORDER BY w.name, ws.step_order;

-- Test functions
SELECT get_unread_notification_count('00000000-0000-0000-0000-000000000002');
```

---

## 🔧 ENVIRONMENT VARIABLES

Add these to your backend application:

```env
DATABASE_URL=postgresql://postgres:[password]@db.[project-ref].supabase.co:5432/postgres
SUPABASE_URL=https://[project-ref].supabase.co
SUPABASE_KEY=[your-anon-key]
SUPABASE_SERVICE_KEY=[your-service-key]
```

---

## ✅ FEATURES INCLUDED

- [x] Complete user management with roles
- [x] Flexible workflow system
- [x] Document lifecycle management
- [x] Signature tracking and history
- [x] Real-time notifications
- [x] Comprehensive audit logging
- [x] Row Level Security (RLS)
- [x] Storage policies for documents
- [x] Automated workflow progression
- [x] Demo data for testing

---

## 🚀 NEXT STEPS

1. **Run all migrations** in Supabase SQL Editor
2. **Configure storage buckets** for document uploads
3. **Test authentication** with demo users
4. **Verify RLS policies** are working correctly
5. **Connect backend application** using environment variables

**Demo Credentials:**
- Admin: `admin@firmador.cr` / `admin123`
- User 1: `juan.perez@ejemplo.cr` / `demo123`
- User 2: `maria.gonzalez@ejemplo.cr` / `demo123`
- User 3: `carlos.martinez@ejemplo.cr` / `demo123`