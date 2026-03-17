/* ============================================================
   Mobile Nav Toggle
   ============================================================ */
(function () {
  const header  = document.querySelector('.site-header');
  const toggle  = document.querySelector('.nav__toggle');
  const menu    = document.querySelector('.nav__menu');

  if (!toggle || !menu || !header) return;

  toggle.addEventListener('click', function () {
    const isOpen = header.classList.toggle('nav-open');
    toggle.setAttribute('aria-expanded', String(isOpen));
  });

  // Close on outside click
  document.addEventListener('click', function (e) {
    if (!header.contains(e.target)) {
      header.classList.remove('nav-open');
      toggle.setAttribute('aria-expanded', 'false');
    }
  });

  // Close when a nav link is clicked (single-page)
  menu.querySelectorAll('.nav__link').forEach(function (link) {
    link.addEventListener('click', function () {
      header.classList.remove('nav-open');
      toggle.setAttribute('aria-expanded', 'false');
    });
  });
}());

/* ============================================================
   Active Nav Link via IntersectionObserver
   ============================================================ */
(function () {
  const sections = document.querySelectorAll('section[id]');
  const navLinks = document.querySelectorAll('.nav__link');

  if (!sections.length || !navLinks.length) return;

  const observer = new IntersectionObserver(function (entries) {
    entries.forEach(function (entry) {
      if (!entry.isIntersecting) return;
      navLinks.forEach(function (link) {
        const href = link.getAttribute('href');
        if (href === '#' + entry.target.id) {
          link.classList.add('active');
        } else {
          link.classList.remove('active');
        }
      });
    });
  }, {
    rootMargin: '-40% 0px -55% 0px',
    threshold: 0
  });

  sections.forEach(function (section) {
    observer.observe(section);
  });
}());

/* ============================================================
   Contact Form — Validation & Success Feedback
   ============================================================ */
(function () {
  const form    = document.querySelector('.contact__form');
  const success = document.getElementById('contact-success');

  if (!form) return;

  function getField(id)  { return document.getElementById(id); }
  function getError(id)  { return document.getElementById(id + '-error'); }

  function showError(fieldId, message) {
    var field = getField(fieldId);
    var error = getError(fieldId);
    if (!field || !error) return;
    error.textContent = message;
    field.setAttribute('aria-invalid', 'true');
  }

  function clearError(fieldId) {
    var field = getField(fieldId);
    var error = getError(fieldId);
    if (!field || !error) return;
    error.textContent = '';
    field.removeAttribute('aria-invalid');
  }

  function isValidEmail(value) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
  }

  function validateForm() {
    var valid = true;

    var name    = getField('contact-name');
    var email   = getField('contact-email');
    var message = getField('contact-message');

    // Name
    if (!name || name.value.trim() === '') {
      showError('contact-name', 'Please enter your full name.');
      valid = false;
    } else {
      clearError('contact-name');
    }

    // Email
    if (!email || email.value.trim() === '') {
      showError('contact-email', 'Please enter your email address.');
      valid = false;
    } else if (!isValidEmail(email.value.trim())) {
      showError('contact-email', 'Please enter a valid email address.');
      valid = false;
    } else {
      clearError('contact-email');
    }

    // Message
    if (!message || message.value.trim() === '') {
      showError('contact-message', 'Please enter a message.');
      valid = false;
    } else if (message.value.trim().length < 10) {
      showError('contact-message', 'Message must be at least 10 characters.');
      valid = false;
    } else {
      clearError('contact-message');
    }

    return valid;
  }

  form.addEventListener('submit', function (e) {
    e.preventDefault();

    if (!validateForm()) return;

    // All valid — show success state, reset form
    form.reset();
    form.querySelectorAll('[aria-invalid]').forEach(function (el) {
      el.removeAttribute('aria-invalid');
    });

    if (success) {
      success.removeAttribute('hidden');
      success.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
  });

  // Clear errors on input change
  ['contact-name', 'contact-email', 'contact-message'].forEach(function (id) {
    var field = getField(id);
    if (field) {
      field.addEventListener('input', function () { clearError(id); });
    }
  });
}());

/* ============================================================
   Footer Year
   ============================================================ */
(function () {
  var yearEl = document.getElementById('footer-year');
  if (yearEl) {
    yearEl.textContent = new Date().getFullYear();
  }
}());
