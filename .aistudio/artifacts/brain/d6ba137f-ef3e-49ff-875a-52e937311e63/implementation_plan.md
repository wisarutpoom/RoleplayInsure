# แผนการเชื่อมโยง Subdomain `roleplay.smartnexus.vip` (ภายใต้ `smartnexus.vip`) และรวมเมนู Admin ในโดเมนเดียว

## 1. ภาพรวมการตั้งค่าโดเมน (ปลอดภัย 100% ไม่กระทบเว็บไซต์หลักเดิม)
จากการตรวจสอบโครงสร้างโดเมน **`smartnexus.vip`** ปัจจุบันของคุณ:
- **โดเมนหลัก (`@`) และ `www.smartnexus.vip`**: ชี้ A Record ไปที่ `104.18.26.246` บน Nameserver `ns1.globaldomaingroup.com` และ `ns2.globaldomaingroup.com` **(ห้ามแก้ไขหรือลบค่าเดิมนี้ เพื่อไม่ให้กระทบระบบหลัก)**
- **Subdomain ที่คุณเลือก**: **`roleplay.smartnexus.vip`**
- **รูปแบบการใช้งาน**: ใช้ลิงก์เดียว **`https://roleplay.smartnexus.vip`** สำหรับทั้งทีมตัวแทนประกัน (เข้าฝึกซ้อม 25 ด่าน) และแอดมิน/เลขาฯ (ล็อกอินด้วย Gmail เพื่อเปิด **เมนู Admin** จากในเว็บแอพเดียวกันได้ทันที)

---

## 2. สิ่งที่จะอัปเดตในระบบแอพและพอร์ทัลเว็บ (`roleplay.smartnexus.vip`)

### 2.1 ตั้งค่าโดเมนมาตรฐานของระบบเป็น `roleplay.smartnexus.vip`
- อัปเดตค่าเริ่มต้นของโดเมนในระบบ (`InsureQuestViewModel.kt`, `WebPortalGenerator.kt`, และ `WebDomainPortalScreen.kt`) ให้เป็น **`roleplay.smartnexus.vip`** โดยอัตโนมัติ
- ปรับข้อความแชร์ลิงก์ให้ทีมงานและอีเมลแจ้งอนุมัติสิทธิ์เลขาฯ ทาง Gmail ให้แสดงลิงก์ **`https://roleplay.smartnexus.vip`** คู่กับลิงก์ Cloud Run หลัก (`https://ais-pre-jdmpizsj2544irqik5aigg-882878485935.asia-southeast1.run.app`)

### 2.2 สร้างตารางคู่มือตั้งค่า DNS สำหรับ `ns1/ns2.globaldomaingroup.com` แบบจับมือทำในหน้า "เว็บ & URL"
- เพิ่มการ์ด **"ตารางตั้งค่า DNS สำหรับ `roleplay.smartnexus.vip` (GlobalDomainGroup)"** ในหน้า `เว็บ & URL` ของแอพ เพื่อให้คุณกดคัดลอกค่าไปวางในหน้าจัดการ DNS ได้ทันทีโดยไม่กระทบ `104.18.26.246`:
  1. **レコードที่ห้ามแตะต้อง**:
     - `@` (`smartnexus.vip`) -> `A` -> `104.18.26.246` *(คงไว้เหมือนเดิม)*
     - `www` (`www.smartnexus.vip`) -> `A` -> `104.18.26.246` *(คงไว้เหมือนเดิม)*
  2. **レコードใหม่ที่เพิ่มเฉพาะ Subdomain `roleplay`**:
     - **Host / Name**: `roleplay` (หรือ `roleplay.smartnexus.vip` ตามรูปแบบแผงควบคุม DNS)
     - **Type**: `A` (และ `TXT` สำหรับยืนยันความเป็นเจ้าของโดเมนจาก Firebase Hosting `gen-lang-client-0907040926`)
     - **Value / Points to**: IP ของ Google Firebase Hosting (`199.36.158.100`) พร้อมออกใบรับรองความปลอดภัย Free SSL (`https://roleplay.smartnexus.vip`) อัตโนมัติ

### 2.3 รวมหน้าเว็บแอพ 25 ด่าน และเมนู Admin/เลขาฯ ไว้ใน `roleplay.smartnexus.vip` ที่เดียว
- อัปเดตโค้ดหน้าเว็บ `index.html` สำหรับ `roleplay.smartnexus.vip` ให้รองรับการเข้าใช้งานครบวงจรในลิงก์เดียว:
  - มีปุ่มสลับ **โหมดมืด / โหมดสว่าง** ธีมไทยสยามอคาเดมี
  - ปุ่มเข้าฝึกซ้อมครบ 25 ด่าน และปุ่มเข้า **เมนู Admin & ตั้งค่าเลขาฯ ผ่าน Gmail** ในหน้าเดียวกัน
  - รองรับการเปิดแบบเต็มจอ (Full-Screen Web App) ทั้งบน iPhone/iPad (Safari), Android (Chrome) และคอมพิวเตอร์
