import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './App.css';

const API_URL = '/api/react';
const EMPTY_STUDENT = { id: null, sname: '', studentClass: '', boy: -1 };
const EMPTY_SUBJECT = { id: null, name: '', category: '' };
const EMPTY_MARK = { id: null, studentid: '', mdste: '', mark: '', type: '', subjectid: '' };
const MARK_ROWS_SHOWN = 100; // the table shows at most this many marks (there are 17,000+)

function App() {
  const [activeTab, setActiveTab] = useState('students');

  // State for Students
  const [students, setStudents] = useState([]);
  const [studentForm, setStudentForm] = useState(EMPTY_STUDENT);

  // State for Subjects
  const [subjects, setSubjects] = useState([]);
  const [subjectForm, setSubjectForm] = useState(EMPTY_SUBJECT);

  // State for Marks
  const [marks, setMarks] = useState([]);
  const [markForm, setMarkForm] = useState(EMPTY_MARK);
  const [markFilter, setMarkFilter] = useState(''); // filter the marks table by student ID

  useEffect(() => {
    fetchStudents();
    fetchSubjects();
    fetchMarks();
  }, []);

  // --- FETCH DATA ---
  const fetchStudents = async () => { const res = await axios.get(`${API_URL}/students`); setStudents(res.data); };
  const fetchSubjects = async () => { const res = await axios.get(`${API_URL}/subjects`); setSubjects(res.data); };
  const fetchMarks = async () => { const res = await axios.get(`${API_URL}/marks`); setMarks(res.data); };

  // --- SUBMIT HANDLERS ---
  // If the form has an id we are editing: PUT (update). Otherwise POST (create).
  const submitStudent = async (e) => {
    e.preventDefault();
    if (studentForm.id) await axios.put(`${API_URL}/students/${studentForm.id}`, studentForm);
    else await axios.post(`${API_URL}/students`, studentForm);
    setStudentForm(EMPTY_STUDENT);
    fetchStudents();
  };

  const submitSubject = async (e) => {
    e.preventDefault();
    if (subjectForm.id) await axios.put(`${API_URL}/subjects/${subjectForm.id}`, subjectForm);
    else await axios.post(`${API_URL}/subjects`, subjectForm);
    setSubjectForm(EMPTY_SUBJECT);
    fetchSubjects();
  };

  const submitMark = async (e) => {
    e.preventDefault();
    if (markForm.id) await axios.put(`${API_URL}/marks/${markForm.id}`, markForm);
    else await axios.post(`${API_URL}/marks`, markForm);
    setMarkForm(EMPTY_MARK);
    fetchMarks();
  };

  // --- EDIT HANDLERS ---
  // Copy the clicked row into the form and scroll up to it.
  const toTop = () => window.scrollTo({ top: 0, behavior: 'smooth' });

  const startEditStudent = (s) => {
    setStudentForm({ id: s.id, sname: s.sname, studentClass: s.studentClass, boy: s.boy ? -1 : 0 });
    toTop();
  };
  const cancelEditStudent = () => setStudentForm(EMPTY_STUDENT);

  const startEditSubject = (s) => {
    setSubjectForm({ id: s.id, name: s.name, category: s.category });
    toTop();
  };
  const cancelEditSubject = () => setSubjectForm(EMPTY_SUBJECT);

  const startEditMark = (m) => {
    setMarkForm({ id: m.id, studentid: m.studentid, mdste: m.mdste, mark: m.mark, type: m.type, subjectid: m.subjectid });
    toTop();
  };
  const cancelEditMark = () => setMarkForm(EMPTY_MARK);

  // --- DELETE HANDLERS ---
  const deleteStudent = async (id) => { await axios.delete(`${API_URL}/students/${id}`); fetchStudents(); };
  const deleteSubject = async (id) => { await axios.delete(`${API_URL}/subjects/${id}`); fetchSubjects(); };
  const deleteMark = async (id) => { await axios.delete(`${API_URL}/marks/${id}`); fetchMarks(); };

  // Marks shown: filter by student ID (optional), newest first, limited to MARK_ROWS_SHOWN rows
  const filteredMarks = marks
    .filter(m => markFilter === '' || String(m.studentid) === markFilter.trim())
    .slice()
    .sort((a, b) => b.id - a.id);
  const shownMarks = filteredMarks.slice(0, MARK_ROWS_SHOWN);

  const inputStyle = { padding: '8px', margin: '5px', borderRadius: '4px', border: '1px solid #ccc' };
  const btnStyle = { padding: '8px 15px', margin: '5px', cursor: 'pointer', backgroundColor: '#a78bfa', color: 'white', border: 'none', borderRadius: '4px' };
  const grayBtn = { ...btnStyle, backgroundColor: '#555' };
  const tableStyle = { borderCollapse: 'collapse', width: '100%', textAlign: 'left', color: 'white' };

  return (
    <div style={{ padding: '20px', fontFamily: 'Arial', color: 'white', backgroundColor: '#0f0c1b', minHeight: '100vh' }}>

      {/* --- BACK BUTTON --- */}
      <a href="/" style={{ display: 'inline-block', marginBottom: '20px', color: '#a78bfa', textDecoration: 'none', fontWeight: 'bold', fontSize: '16px' }}>
        ← Back to ZenithFX Home
      </a>

      <h2>ZenithFX - Class Record Database</h2>

      {/* TAB MENU */}
      <div style={{ marginBottom: '20px' }}>
        <button onClick={() => setActiveTab('students')} style={{...btnStyle, backgroundColor: activeTab === 'students' ? '#a78bfa' : '#333'}}>Students</button>
        <button onClick={() => setActiveTab('subjects')} style={{...btnStyle, backgroundColor: activeTab === 'subjects' ? '#a78bfa' : '#333'}}>Subjects</button>
        <button onClick={() => setActiveTab('marks')} style={{...btnStyle, backgroundColor: activeTab === 'marks' ? '#a78bfa' : '#333'}}>Marks</button>
      </div>

      {/* STUDENTS TAB */}
      {activeTab === 'students' && (
        <div>
          <form onSubmit={submitStudent} style={{ marginBottom: '20px' }}>
            <input style={inputStyle} type="text" placeholder="Student Name" value={studentForm.sname} onChange={(e) => setStudentForm({...studentForm, sname: e.target.value})} required />
            <input style={inputStyle} type="text" placeholder="Class (e.g. 12/B)" value={studentForm.studentClass} onChange={(e) => setStudentForm({...studentForm, studentClass: e.target.value})} required />
            <select style={inputStyle} value={studentForm.boy} onChange={(e) => setStudentForm({...studentForm, boy: parseInt(e.target.value)})}>
              <option value={-1}>Boy</option>
              <option value={0}>Girl</option>
            </select>
            <button style={btnStyle} type="submit">{studentForm.id ? 'Save Changes' : 'Add Student'}</button>
            {studentForm.id && <button style={grayBtn} type="button" onClick={cancelEditStudent}>Cancel</button>}
          </form>
          <table border="1" cellPadding="10" style={tableStyle}>
            <thead><tr><th>ID</th><th>Name</th><th>Class</th><th>Boy/Girl</th><th>Actions</th></tr></thead>
            <tbody>
              {students.map(s => (
                <tr key={s.id}>
                  <td>{s.id}</td><td>{s.sname}</td><td>{s.studentClass}</td><td>{s.boy ? 'Boy' : 'Girl'}</td>
                  <td>
                    <button style={btnStyle} onClick={() => startEditStudent(s)}>Edit</button>
                    <button style={btnStyle} onClick={() => deleteStudent(s.id)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* SUBJECTS TAB */}
      {activeTab === 'subjects' && (
        <div>
          <form onSubmit={submitSubject} style={{ marginBottom: '20px' }}>
            <input style={inputStyle} type="text" placeholder="Subject Name" value={subjectForm.name} onChange={(e) => setSubjectForm({...subjectForm, name: e.target.value})} required />
            <input style={inputStyle} type="text" placeholder="Category" value={subjectForm.category} onChange={(e) => setSubjectForm({...subjectForm, category: e.target.value})} required />
            <button style={btnStyle} type="submit">{subjectForm.id ? 'Save Changes' : 'Add Subject'}</button>
            {subjectForm.id && <button style={grayBtn} type="button" onClick={cancelEditSubject}>Cancel</button>}
          </form>
          <table border="1" cellPadding="10" style={tableStyle}>
            <thead><tr><th>ID</th><th>Name</th><th>Category</th><th>Actions</th></tr></thead>
            <tbody>
              {subjects.map(s => (
                <tr key={s.id}>
                  <td>{s.id}</td><td>{s.name}</td><td>{s.category}</td>
                  <td>
                    <button style={btnStyle} onClick={() => startEditSubject(s)}>Edit</button>
                    <button style={btnStyle} onClick={() => deleteSubject(s.id)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* MARKS TAB */}
      {activeTab === 'marks' && (
        <div>
          <form onSubmit={submitMark} style={{ marginBottom: '20px' }}>
            <input style={inputStyle} type="number" placeholder="Student ID" value={markForm.studentid} onChange={(e) => setMarkForm({...markForm, studentid: e.target.value})} required />
            <input style={inputStyle} type="date" value={markForm.mdste} onChange={(e) => setMarkForm({...markForm, mdste: e.target.value})} required />
            <input style={inputStyle} type="number" placeholder="Mark (1-5)" value={markForm.mark} onChange={(e) => setMarkForm({...markForm, mark: e.target.value})} required />
            <input style={inputStyle} type="text" placeholder="Type (e.g. Test)" value={markForm.type} onChange={(e) => setMarkForm({...markForm, type: e.target.value})} required />
            <input style={inputStyle} type="number" placeholder="Subject ID" value={markForm.subjectid} onChange={(e) => setMarkForm({...markForm, subjectid: e.target.value})} required />
            <button style={btnStyle} type="submit">{markForm.id ? 'Save Changes' : 'Add Mark'}</button>
            {markForm.id && <button style={grayBtn} type="button" onClick={cancelEditMark}>Cancel</button>}
          </form>

          {/* FILTER (there are 17,000+ marks, so the table shows only a part of them) */}
          <div style={{ marginBottom: '10px' }}>
            <input style={inputStyle} type="number" placeholder="Filter by Student ID" value={markFilter} onChange={(e) => setMarkFilter(e.target.value)} />
            <span style={{ color: '#8b8aa0', fontSize: '13px' }}>
              Showing {shownMarks.length} of {filteredMarks.length} marks (newest first)
            </span>
          </div>

          <table border="1" cellPadding="10" style={tableStyle}>
            <thead><tr><th>ID</th><th>Student ID</th><th>Date</th><th>Mark</th><th>Type</th><th>Subject ID</th><th>Actions</th></tr></thead>
            <tbody>
              {shownMarks.map(m => (
                <tr key={m.id}>
                  <td>{m.id}</td><td>{m.studentid}</td><td>{m.mdste}</td><td>{m.mark}</td><td>{m.type}</td><td>{m.subjectid}</td>
                  <td>
                    <button style={btnStyle} onClick={() => startEditMark(m)}>Edit</button>
                    <button style={btnStyle} onClick={() => deleteMark(m.id)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* FOOTER (creator name and Neptun code, required on every page) */}
      <footer style={{ marginTop: '40px', textAlign: 'center', color: '#8b8aa0', fontSize: '13px' }}>
        © 2026 ZenithFX · Academic project · Abdurrahman Umar Baba - Cw6nlt
      </footer>

    </div>
  );
}

export default App;